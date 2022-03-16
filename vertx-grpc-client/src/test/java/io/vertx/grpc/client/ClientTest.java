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

import io.grpc.Grpc;
import io.grpc.ServerCredentials;
import io.grpc.Status;
import io.grpc.TlsServerCredentials;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientTest extends GrpcTestBase {

  private static final int NUM_ITEMS = 128;

  @Override
  public void tearDown(TestContext should) {
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) throws IOException {
    testUnary(should, "identity", "identity");
  }

  @Test
  public void testUnaryDecompression(TestContext should) throws IOException {
    testUnary(should, "identity", "gzip");
  }

  @Test
  public void testUnaryCompression(TestContext should) throws IOException {
    testUnary(should, "gzip", "identity");
  }

  private void testUnary(TestContext should, String requestEncoding, String responseEncoding) throws IOException {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> plainResponseObserver) {
        ServerCallStreamObserver<HelloReply> responseObserver =
          (ServerCallStreamObserver<HelloReply>) plainResponseObserver;
        responseObserver.setCompression(responseEncoding);
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };
    startServer(called);

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpc.getSayHelloMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.encoding(requestEncoding);
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(reply -> {
            should.assertEquals(1, count.incrementAndGet());
            should.assertEquals("Hello Julien", reply.getMessage());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(1, count.get());
            test.complete();
          });
        }));
        callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
      }));
  }

  @Test
  public void testSSL(TestContext should) throws IOException {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };

    SelfSignedCertificate cert = SelfSignedCertificate.create();
    ServerCredentials creds = TlsServerCredentials
      .newBuilder()
      .keyManager(new File(cert.certificatePath()), new File(cert.privateKeyPath()))
      .build();
    startServer(called, Grpc.newServerBuilderForPort(8443, creds));

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx, new HttpClientOptions().setSsl(true)
      .setUseAlpn(true)
      .setPemTrustOptions(cert.trustOptions()));
    client.request(SocketAddress.inetSocketAddress(8443, "localhost"), GreeterGrpc.getSayHelloMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(reply -> {
            should.assertEquals(1, count.incrementAndGet());
            should.assertEquals("Hello Julien", reply.getMessage());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(1, count.get());
            test.complete();
          });
        }));
        callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
      }));
  }

  @Test
  public void testStatus(TestContext should) throws IOException {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onError(Status.UNAVAILABLE
          .withDescription("Greeter temporarily unavailable...").asRuntimeException());
      }
    };
    startServer(called);

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpc.getSayHelloMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          callResponse.messageHandler(reply -> {
            should.fail();
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.UNAVAILABLE, callResponse.status());
            test.complete();
          });
        }));
        callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
      }));
  }

  @Test
  public void testServerStreaming(TestContext should) throws IOException {

    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public void source(Empty request, StreamObserver<Item> responseObserver) {
        for (int i = 0;i < NUM_ITEMS;i++) {
          responseObserver.onNext(Item.newBuilder().setValue("the-value-" + i).build());
        }
        responseObserver.onCompleted();
      }
    });

    final Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSourceMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            int i = count.getAndIncrement();
            should.assertEquals("the-value-" + i, item.getValue());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(NUM_ITEMS, count.get());
            test.complete();
          });
        }));
        callRequest.end(Empty.getDefaultInstance());
      }));
  }

  @Test
  public void testClientStreaming(TestContext should) throws IOException {

    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> sink(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<Item>() {
          final List<String> items = new ArrayList<>();
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
            List<String> expected = IntStream.rangeClosed(0, NUM_ITEMS - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };
      }
    });

    Async done = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSinkMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            count.incrementAndGet();
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(1, count.get());
            done.complete();
          });
        }));
        AtomicInteger count = new AtomicInteger(NUM_ITEMS);
        vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            callRequest.write(Item.newBuilder().setValue("the-value-" + (NUM_ITEMS - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            callRequest.end();
          }
        });
      }));
  }

  @Test
  public void testBidiStreaming(TestContext should) throws IOException {

    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> pipe(StreamObserver<Item> responseObserver) {
        return responseObserver;
      }
    });

    Async done = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    client.request(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getPipeMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            int i = count.getAndIncrement();
            should.assertEquals("the-value-" + i, item.getValue());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(GrpcStatus.OK, callResponse.status());
            should.assertEquals(NUM_ITEMS, count.get());
            done.complete();
          });
        }));
        AtomicInteger count = new AtomicInteger(NUM_ITEMS);
        vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            callRequest.write(Item.newBuilder().setValue("the-value-" + (NUM_ITEMS - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            callRequest.end();
          }
        });
      }));
  }
}
