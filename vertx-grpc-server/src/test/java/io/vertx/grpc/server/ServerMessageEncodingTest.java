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
package io.vertx.grpc.server;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ServerMessageEncodingTest extends ServerTestBase {

  @Test
  public void testZipResponseCompress(TestContext should) {
    testEncode(should, "gzip", GrpcMessage.message("identity", Buffer.buffer("Hello World")), true);
  }

  @Test
  public void testZipResponsePassThrough(TestContext should) {
    testEncode(should, "gzip", GrpcMessage.message("gzip", zip(Buffer.buffer("Hello World"))), true);
  }

  @Test
  public void testIdentityResponseUnzip(TestContext should) {
    testEncode(should, "identity", GrpcMessage.message("gzip", zip(Buffer.buffer("Hello World"))), false);
  }

  @Test
  public void testIdentityRequestPassThrough(TestContext should) {
    testEncode(should, "identity", GrpcMessage.message("identity", Buffer.buffer("Hello World")), false);
  }

  private void testEncode(TestContext should, String encoding, GrpcMessage msg, boolean compressed) {

    Buffer expected = Buffer.buffer("Hello World");

    startServer(GrpcServer.server().callHandler(call -> {
      call.handler(request -> {
        GrpcServerResponse<Buffer, Buffer> response = call.response();
        response
          .encoding(encoding)
          .endMessage(msg);
      });
    }));

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(true)
    );

    Async done = should.async();

    client.request(HttpMethod.POST, 8080, "localhost", "/", should.asyncAssertSuccess(request -> {
      request.putHeader("grpc-encoding", "identity");
      request.send(Buffer
        .buffer()
        .appendByte((byte)1)
        .appendInt(expected.length())
        .appendBuffer(expected), should.asyncAssertSuccess(resp -> {
          resp.body(should.asyncAssertSuccess(body -> {
            should.assertEquals(compressed ? 1 : 0, (int)body.getByte(0));
            int len = body.getInt(1);
            Buffer received = body.slice(5, 5 + len);
            if (compressed) {
              received = unzip(received);
            }
            should.assertEquals(expected, received);
            done.complete();
          }));
      }));
    }));
  }

  @Test
  public void testDecodeMessageHandler(TestContext should) throws Exception {
    testDecode(should, callResponse -> {
      AtomicInteger count = new AtomicInteger();
      callResponse.messageHandler(msg -> {
        should.assertEquals("gzip", msg.encoding());
        should.assertEquals(Buffer.buffer("Hello World"), unzip(msg.payload()));
        count.incrementAndGet();
      });
      callResponse.endHandler(v -> {
        should.assertEquals(1, count.get());
        callResponse.response().end();
      });
    });
  }

  @Test
  public void testDecodeHandler(TestContext should) throws Exception {
    testDecode(should, callResponse -> {
      AtomicInteger count = new AtomicInteger();
      callResponse.handler(msg -> {
        should.assertEquals(Buffer.buffer("Hello World"), msg);
        count.incrementAndGet();
      });
      callResponse.endHandler(v -> {
        should.assertEquals(1, count.get());
        callResponse.response().end();
      });
    });
  }
  private void testDecode(TestContext should, Consumer<GrpcServerRequest<Buffer, Buffer>> check) throws Exception {

    Buffer expected = Buffer.buffer("Hello World");

    startServer(GrpcServer.server().callHandler(call -> {
      should.assertEquals("gzip", call.encoding());
      check.accept(call);
    }));

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(true)
    );

    client.request(HttpMethod.POST, 8080, "localhost", "/", should.asyncAssertSuccess(request -> {
      request.putHeader("grpc-encoding", "gzip");
      Buffer zipped = zip(expected);
      request.send(Buffer
        .buffer()
        .appendByte((byte)1)
        .appendInt(zipped.length())
        .appendBuffer(zipped), should.asyncAssertSuccess(resp -> {
        resp.body(should.asyncAssertSuccess(body -> {
        }));
      }));
    }));
  }
}
