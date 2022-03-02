package io.vertx.ext.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerCallResponse;
import io.vertx.grpc.server.GrpcStatus;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ClientTest extends GrpcTestBase2 {

  private static final int NUM_ITEMS = 128;

  @Override
  public void tearDown(TestContext should) {
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) {
    testUnary(should, "identity", "identity");
  }

  @Test
  public void testUnaryDecompression(TestContext should) {
    testUnary(should, "identity", "gzip");
  }

  @Test
  public void testUnaryCompression(TestContext should) {
    testUnary(should, "gzip", "identity");
  }

  private void testUnary(TestContext should, String requestEncoding, String responseEncoding) {

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
    GrpcClient client = new GrpcClient(vertx);
    client.call(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpc.getSayHelloMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.encoding(requestEncoding);
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(reply -> {
            should.assertEquals(1, count.incrementAndGet());
            should.assertEquals("Hello Julien", reply.getMessage());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(1, count.get());
            test.complete();
          });
        }));
        callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
      }));
  }

  @Test
  public void testSSL(TestContext should) {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };
    startServer(called, VertxServerBuilder.forPort(vertx, 8443).useSsl(options -> options
      .setSsl(true)
      .setUseAlpn(true)
      .setKeyStoreOptions(new JksOptions()
        .setPath("tls/server-keystore.jks")
        .setPassword("wibble"))));

    Async test = should.async();
    GrpcClient client = new GrpcClient(new HttpClientOptions().setSsl(true)
      .setUseAlpn(true)
      .setTrustStoreOptions(new JksOptions()
        .setPath("tls/client-truststore.jks")
        .setPassword("wibble")), vertx);
    client.call(SocketAddress.inetSocketAddress(8443, "localhost"), GreeterGrpc.getSayHelloMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(reply -> {
            should.assertEquals(1, count.incrementAndGet());
            should.assertEquals("Hello Julien", reply.getMessage());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(1, count.get());
            test.complete();
          });
        }));
        callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
      }));
  }

  @Test
  public void testStatus(TestContext should) {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onError(Status.UNAVAILABLE
          .withDescription("Greeter temporarily unavailable...").asRuntimeException());
      }
    };
    startServer(called);

    Async test = should.async();
    GrpcClient client = new GrpcClient(vertx);
    client.call(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpc.getSayHelloMethod())
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
  public void testServerStreaming(TestContext should) {

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
    GrpcClient client = new GrpcClient(vertx);
    client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSourceMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            int i = count.getAndIncrement();
            should.assertEquals("the-value-" + i, item.getValue());
          });
          callResponse.endHandler(v2 -> {
            should.assertEquals(NUM_ITEMS, count.get());
            test.complete();
          });
        }));
        callRequest.end(Empty.getDefaultInstance());
      }));
  }

  @Test
  public void testClientStreaming(TestContext should) {

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
    GrpcClient client = new GrpcClient(vertx);
    client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSinkMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            count.incrementAndGet();
          });
          callResponse.endHandler(v2 -> {
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
  public void testBidiStreaming(TestContext should) {

    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> pipe(StreamObserver<Item> responseObserver) {
        return responseObserver;
      }
    });

    Async done = should.async();
    GrpcClient client = new GrpcClient(vertx);
    client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getPipeMethod())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
          AtomicInteger count = new AtomicInteger();
          callResponse.messageHandler(item -> {
            int i = count.getAndIncrement();
            should.assertEquals("the-value-" + i, item.getValue());
          });
          callResponse.endHandler(v2 -> {
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
