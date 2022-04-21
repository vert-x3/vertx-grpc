/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.client;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.MessageDecoder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientMessageEncodingTest extends GrpcTestBase {

  private static Buffer unzip(Buffer buffer) {
    Buffer ret = Buffer.buffer();
    try {
    GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(buffer.getBytes()));
    byte[] tmp = new byte[256];
    for (int l = 0;l != -1;l = in.read(tmp)) {
      ret.appendBytes(tmp, 0, l);
    }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ret;
  }

  private static Buffer zip(Buffer buffer) {
    ByteArrayOutputStream ret = new ByteArrayOutputStream();
    try {
      GZIPOutputStream in = new GZIPOutputStream(ret);
      in.write(buffer.getBytes());
      in.flush();
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Buffer.buffer(ret.toByteArray());
  }

  @Test
  public void testZipRequestCompress(TestContext should) throws Exception {
    testEncode(should, "gzip", new GrpcMessage() {
      @Override
      public String encoding() {
        return "identity";
      }
      @Override
      public Buffer payload() {
        return Buffer.buffer("Hello World");
      }
    }, true);
  }

  @Test
  public void testZipRequestPassThrough(TestContext should) throws Exception {
    testEncode(should, "gzip", new GrpcMessage() {
      @Override
      public String encoding() {
        return "gzip";
      }
      @Override
      public Buffer payload() {
        return zip(Buffer.buffer("Hello World"));
      }
    }, true);
  }

  @Test
  public void testIdentityRequestUnzip(TestContext should) throws Exception {
    testEncode(should, "identity", new GrpcMessage() {
      @Override
      public String encoding() {
        return "gzip";
      }
      @Override
      public Buffer payload() {
        return zip(Buffer.buffer("Hello World"));
      }
    }, false);
  }

  @Test
  public void testIdentityRequestPassThrough(TestContext should) throws Exception {
    testEncode(should, "identity", new GrpcMessage() {
      @Override
      public String encoding() {
        return "identity";
      }
      @Override
      public Buffer payload() {
        return Buffer.buffer("Hello World");
      }
    }, false);
  }

  private void testEncode(TestContext should, String requestEncoding, GrpcMessage msg, boolean compressed) throws Exception {

    Buffer expected = Buffer.buffer("Hello World");

    vertx.createHttpServer().requestHandler(req -> {
      req.bodyHandler(body -> {
        should.assertEquals(compressed ? 1 : 0, (int)body.getByte(0));
        Buffer payload = body.slice(5, body.length());
        if (compressed) {
          payload = unzip(payload);
        }
        should.assertEquals(expected, payload);
        req.response()
          .putHeader("grpc-status", "" + GrpcStatus.CANCELLED.code)
          .end();
      });
    }).listen(8080, "localhost")
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"))
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.fullMethodName(GreeterGrpc.getSayHelloMethod().getFullMethodName());
        callRequest.encoding(requestEncoding);
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          should.assertEquals(GrpcStatus.CANCELLED, callResponse.status());
          test.complete();
        }));
        callRequest.endMessage(msg);
      }));
  }

  @Test
  public void testDecode(TestContext should) throws Exception {

    Buffer expected = Buffer.buffer("Hello World");

    vertx.createHttpServer().requestHandler(req -> {
        req.endHandler(v -> {
          HttpServerResponse resp = req.response();
          resp.putHeader("grpc-encoding", "gzip");
          resp.putTrailer("grpc-status", "" + GrpcStatus.OK.code);
          Buffer prefix = Buffer.buffer();
          prefix.appendByte((byte)1); // Compressed
          Buffer payload = zip(expected);
          prefix.appendInt(payload.length());
          resp.write(prefix);
          resp.write(payload);
          resp.end();
        });
      }).listen(8080, "localhost")
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"))
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.fullMethodName(GreeterGrpc.getSayHelloMethod().getFullMethodName());
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(msg -> {
            should.assertEquals("gzip", msg.encoding());
            count.incrementAndGet();
          });
          callResponse.endHandler(v -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(1, count.get());
            test.complete();
          });
        }));
        callRequest.end(Buffer.buffer());
      }));
  }
}
