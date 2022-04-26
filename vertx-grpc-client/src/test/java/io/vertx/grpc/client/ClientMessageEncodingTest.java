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

import io.grpc.Metadata;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientMessageEncodingTest extends ClientTestBase {

  @Test
  public void testZipRequestCompress(TestContext should) throws Exception {
    testEncode(should, "gzip", GrpcMessage.message("identity", Buffer.buffer("Hello World")), true);
  }

  @Test
  public void testZipRequestPassThrough(TestContext should) throws Exception {
    testEncode(should, "gzip", GrpcMessage.message("gzip", zip(Buffer.buffer("Hello World"))), true);
  }

  @Test
  public void testIdentityRequestUnzip(TestContext should) throws Exception {
    testEncode(should, "identity", GrpcMessage.message("gzip", zip(Buffer.buffer("Hello World"))), false);
  }

  @Test
  public void testIdentityRequestPassThrough(TestContext should) throws Exception {
    testEncode(should, "identity", GrpcMessage.message("identity", Buffer.buffer("Hello World")), false);
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
  public void testEncodeError(TestContext should) throws Exception {

    vertx.createHttpServer().requestHandler(req -> {
      should.fail();
      }).listen(8080, "localhost")
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);

    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"))
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.fullMethodName(GreeterGrpc.getSayHelloMethod().getFullMethodName());
        callRequest.encoding("identity");
        List<GrpcMessage> messages = Arrays.asList(
          GrpcMessage.message("gzip", Buffer.buffer("Hello World")),
          GrpcMessage.message("gzip", Buffer.buffer("not-gzip")),
          GrpcMessage.message("unknown", Buffer.buffer("unknown"))
        );
        for (GrpcMessage message : messages) {
          Future<Void> fut = callRequest.writeMessage(message);
          fut.onComplete(should.asyncAssertFailure());
        }
      }));
  }

  @Test
  public void testDecodeMessageHandler(TestContext should) throws Exception {
    Async done = should.async();
    testDecode(should, zip(Buffer.buffer("Hello World")), callResponse -> {
      callResponse.messageHandler(msg -> {
        should.assertEquals("gzip", msg.encoding());
        should.assertEquals(Buffer.buffer("Hello World"), unzip(msg.payload()));
        done.complete();
      });
    }, req -> {});
  }

  @Test
  public void testDecodeHandler(TestContext should) throws Exception {
    Async done = should.async();
    testDecode(should, zip(Buffer.buffer("Hello World")), callResponse -> {
      callResponse.handler(msg -> {
        should.assertEquals(Buffer.buffer("Hello World"), msg);
        done.complete();
      });
    }, req -> {});
  }

  @Test
  public void testDecodeError(TestContext should) throws Exception {
    Async done = should.async();
    testDecode(should, Buffer.buffer("Hello World"), callResponse -> {
      callResponse.handler(msg -> {
        should.fail();
      });
    }, req -> {
      req.response().exceptionHandler(err -> {
        if (err instanceof StreamResetException) {
          StreamResetException reset = (StreamResetException) err;
          should.assertEquals(GrpcError.CANCELLED.http2ResetCode, reset.getCode());
          done.complete();
        }
      });
    });
  }

  private void testDecode(TestContext should, Buffer payload, Consumer<GrpcClientResponse<Buffer, Buffer>> impl, Consumer<HttpServerRequest> checker) throws Exception {

    vertx.createHttpServer().requestHandler(req -> {
        req.endHandler(v -> {
          HttpServerResponse resp = req.response();
          resp.putHeader("grpc-encoding", "gzip");
          resp.putTrailer("grpc-status", "" + GrpcStatus.OK.code);
          resp.write(Buffer.buffer()
            .appendByte((byte)1)
            .appendInt(payload.length())
            .appendBuffer(payload));
        });
        checker.accept(req);
      }).listen(8080, "localhost")
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);

    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"))
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.fullMethodName(GreeterGrpc.getSayHelloMethod().getFullMethodName());
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          should.assertEquals("gzip", callResponse.encoding());
          impl.accept(callResponse);
        }));
        callRequest.end(Buffer.buffer());
      }));
  }

  // A test to check, gRPC implementation behavior
  @Test
  public void testServerDecodingError(TestContext should) throws Exception {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> plainResponseObserver) {
        ServerCallStreamObserver<HelloReply> responseObserver =
          (ServerCallStreamObserver<HelloReply>) plainResponseObserver;
      }
    };
    startServer(called, ServerBuilder.forPort(port).intercept(new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(call, headers);
      }
    }));

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(false)
    );

    Async async = should.async();

    client.request(HttpMethod.POST, 8080, "localhost", "/" + GreeterGrpc.getSayHelloMethod().getFullMethodName(), should.asyncAssertSuccess(request -> {
      request.putHeader("content-type", "application/grpc");
      request.putHeader("grpc-encoding", "gzip");
      request.sendHead();
      request.send(Buffer.buffer().appendByte((byte)1).appendInt(11).appendString("Hello World"))
        .onComplete(should.asyncAssertFailure(err -> {
          should.assertEquals(StreamResetException.class, err.getClass());
          StreamResetException reset = (StreamResetException) err;
          should.assertEquals(GrpcError.CANCELLED.http2ResetCode, reset.getCode());
          async.complete();
        }));
    }));
  }
}
