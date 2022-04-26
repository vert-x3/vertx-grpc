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

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ServerRequestTest extends ServerTestBase {

  @Override
  protected void testUnary(TestContext should, String requestEncoding, String responseEncoding) {
    startServer(GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        if (!requestEncoding.equals("identity")) {
          should.assertEquals(requestEncoding, call.encoding());
        }
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response
          .encoding(responseEncoding)
          .end(helloReply);
      });
    }));

    super.testUnary(should, requestEncoding, responseEncoding);
  }

  @Test
  public void testSSL(TestContext should) throws IOException {

    SelfSignedCertificate cert = SelfSignedCertificate.create();

    startServer(new HttpServerOptions()
      .setSsl(true)
      .setUseAlpn(true)
      .setPort(8443)
      .setHost("localhost")
      .setPemKeyCertOptions(cert.keyCertOptions()), GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response
          .end(helloReply);
      });
    }));

    ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(new File(cert.certificatePath())).build();
    channel = Grpc.newChannelBuilderForAddress("localhost", 8443, creds).build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    HelloReply res = stub.sayHello(request);
    should.assertEquals("Hello Julien", res.getMessage());
  }

  @Override
  public void testStatus(TestContext should) {

    startServer(GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response
          .status(GrpcStatus.UNAVAILABLE)
          .end();
      });
    }));

    super.testStatus(should);
  }

  @Override
  public void testServerStreaming(TestContext should) {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getSourceMethod(), call -> {
      for (int i = 0; i < NUM_ITEMS; i++) {
        Item item = Item.newBuilder().setValue("the-value-" + i).build();
        call.response().write(item);
      }
      call.response().end();
    }));

    super.testServerStreaming(should);
  }

  @Override
  public void testClientStreaming(TestContext should) throws Exception {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.handler(item -> {
        // Should assert item
      });
      call.endHandler(v -> {
        call.response().end(Empty.getDefaultInstance());
      });
    }));

    super.testClientStreaming(should);
  }

  @Override
  public void testClientStreamingCompletedBeforeHalfClose(TestContext should) {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.handler(item -> {
        call.response().status(GrpcStatus.CANCELLED).end();
      });
      call.endHandler(v -> {
        should.fail();
      });
    }));

    super.testClientStreamingCompletedBeforeHalfClose(should);
  }

  @Override
  public void testBidiStreaming(TestContext should) throws Exception {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.handler(item -> {
        call.response().write(item);
      });
      call.endHandler(v -> {
        call.response().end();
      });
    }));

    super.testBidiStreaming(should);
  }

  @Override
  public void testBidiStreamingCompletedBeforeHalfClose(TestContext should) throws Exception {

    Async done = should.async();
    startServer(GrpcServer.server().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.handler(item -> {
        call.response().end();
        call.errorHandler(err -> {
          should.assertEquals(GrpcError.CANCELLED, err);
          done.complete();
        });
      });
    }));

    super.testBidiStreamingCompletedBeforeHalfClose(should);
  }

  @Test
  public void testMetadata(TestContext should) {

    startServer(GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      should.assertEquals(0, testMetadataStep.getAndIncrement());
      should.assertEquals("custom_request_header_value", call.headers().get("custom_request_header"));
      call.handler(helloRequest -> {
        should.assertEquals(1, testMetadataStep.getAndAdd(2));
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response.headers().set("custom_response_header", "custom_response_header_value");
        response.trailers().set("custom_response_trailer", "custom_response_trailer_value");
        response
          .end(helloReply);
      });
    }));

    super.testMetadata(should);
  }

  @Test
  public void testFailInHeaders(TestContext should) {
    testFail(should, 0);
  }

  @Test
  public void testFailInTrailers(TestContext should) {
    testFail(should, 1);
  }

  private void testFail(TestContext should, int numMsg) {
    startServer(GrpcServer.server().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.handler(item -> {
        for (int i = 0;i < numMsg;i++) {
          call.response().write(item);
        }
        call.response().status(GrpcStatus.UNAVAILABLE).end();
      });
    }));

    channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);

    Async done = should.async();
    ClientCallStreamObserver<Item> items = (ClientCallStreamObserver<Item>) stub.pipe(new StreamObserver<Item>() {
      AtomicInteger count = new AtomicInteger();
      @Override
      public void onNext(Item value) {
        count.getAndIncrement();
      }
      @Override
      public void onError(Throwable t) {
        should.assertEquals(StatusRuntimeException.class, t.getClass());
        StatusRuntimeException sre = (StatusRuntimeException) t;
        should.assertEquals(Status.UNAVAILABLE.getCode(), sre.getStatus().getCode());
        should.assertEquals(numMsg, count.get());
        done.complete();
      }
      @Override
      public void onCompleted() {
      }
    });
    items.onNext(Item.newBuilder().setValue("the-value").build());
  }

  @Test
  public void testHandleReset(TestContext should) {

    Async test = should.async();
    startServer(GrpcServer.server().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.errorHandler(error -> {
        should.assertEquals(GrpcError.CANCELLED, error);
        test.complete();
      });
      call.handler(item -> {
        call.response().write(item);
      });
    }));

    super.testHandleReset(should);
  }
}
