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
import io.grpc.ManagedChannel;
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
import io.grpc.stub.StreamObserver;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ServerTest extends GrpcTestBase {

  private static final int NUM_ITEMS = 128;
  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) {
    testUnary(should, "identity", "identity");
  }

  @Test
  public void testUnaryDecompression(TestContext should) {
    testUnary(should, "gzip", "identity");
  }

  @Test
  public void testUnaryCompression(TestContext should) {
    testUnary(should, "identity", "gzip");
  }

  private void testUnary(TestContext should, String requestEncoding, String responseEncoding) {

    startServer(GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response
          .encoding(responseEncoding)
          .end(helloReply);
      });
    }));

    channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel).withCompression(requestEncoding);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    HelloReply res = stub.sayHello(request);
    should.assertEquals("Hello Julien", res.getMessage());
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

  @Test
  public void testStatus(TestContext should) {

    startServer(GrpcServer.server().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        GrpcServerResponse<HelloRequest, HelloReply> response = call.response();
        response
          .status(GrpcStatus.UNAVAILABLE)
          .end();
      });
    }));

    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    channel = ManagedChannelBuilder.forAddress( "localhost", port)
      .usePlaintext()
      .build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
    try {
      stub.sayHello(request);
    } catch (StatusRuntimeException e) {
      should.assertEquals(Status.UNAVAILABLE, e.getStatus());
    }
  }

  @Test
  public void testServerStreaming(TestContext should) {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getSourceMethod(), call -> {
      for (int i = 0; i < NUM_ITEMS; i++) {
        Item item = Item.newBuilder().setValue("the-value-" + i).build();
        call.response().write(item);
      }
      call.response().end();
    }));

    channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingBlockingStub stub = StreamingGrpc.newBlockingStub(channel);

    List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build()).forEachRemaining(item -> items.add(item.getValue()));
    List<String> expected = IntStream.rangeClosed(0, NUM_ITEMS - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
    should.assertEquals(expected, items);
  }

  @Test
  public void testClientStreaming(TestContext should) throws Exception {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.handler(item -> {
        // Should assert item
      });
      call.endHandler(v -> {
        call.response().end(Empty.getDefaultInstance());
      });
    }));

    channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);

    Async test = should.async();
    StreamObserver<Item> items = stub.sink(new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
      }
      @Override
      public void onError(Throwable t) {
        should.fail(t);
      }
      @Override
      public void onCompleted() {
        test.complete();
      }
    });
    for (int i = 0; i < NUM_ITEMS; i++) {
      items.onNext(Item.newBuilder().setValue("the-value-" + i).build());
      Thread.sleep(10);
    }
    items.onCompleted();
  }

  @Test
  public void testBidiStreaming(TestContext should) throws Exception {

    startServer(GrpcServer.server().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.handler(item -> {
        call.response().write(item);
      });
      call.endHandler(v -> {
        call.response().end();
      });
    }));

    channel = ManagedChannelBuilder.forAddress("localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);

    Async test = should.async();
    List<String> items = new ArrayList<>();
    StreamObserver<Item> writer = stub.pipe(new StreamObserver<Item>() {
      @Override
      public void onNext(Item item) {
        items.add(item.getValue());
      }
      @Override
      public void onError(Throwable t) {
        should.fail(t);
      }
      @Override
      public void onCompleted() {
        test.complete();
      }
    });
    for (int i = 0; i < NUM_ITEMS; i++) {
      writer.onNext(Item.newBuilder().setValue("the-value-" + i).build());
      Thread.sleep(10);
    }
    writer.onCompleted();
    test.awaitSuccess(20_000);
    List<String> expected = IntStream.rangeClosed(0, NUM_ITEMS - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
    should.assertEquals(expected, items);
  }
}
