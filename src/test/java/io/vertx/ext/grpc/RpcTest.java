package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.StreamObserver;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.*;
import org.junit.Test;

import io.grpc.examples.helloworld.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class RpcTest extends GrpcTestBase {

  @Test
  public void testSimple(TestContext ctx) throws Exception {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v -> startServer(new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    }, ar -> {
      if (ar.succeeded()) {
        started.complete();
      } else {
        ctx.fail(ar.cause());
      }
    }));
    started.awaitSuccess(10000);
    Async async = ctx.async();
    Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
      GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, new StreamObserver<HelloReply>() {
        private HelloReply result;
        @Override
        public void onNext(HelloReply helloReply) {
          result = helloReply;
        }
        @Override
        public void onError(Throwable throwable) {
          ctx.fail(throwable);
          channel.shutdown();
        }
        @Override
        public void onCompleted() {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", result.getMessage());
          async.complete();
          channel.shutdown();
        }
      });
    });
  }

  @Test
  public void testBlocking(TestContext ctx) {
    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        // run on worker
        ctx.assertTrue(Context.isOnWorkerThread());
        System.out.println("sleep on " + Thread.currentThread());
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return h.startCall(call, m);
      }
    };
    BindableService service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        ctx.assertTrue(Context.isOnEventLoopThread());
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
        responseObserver.onCompleted();
      }
    };
    Async started = ctx.async();
    server = VertxServerBuilder.forPort(vertx, port)
      .addService(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .build()
      .start(ar -> {
        if (ar.succeeded()) {
          started.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    started.awaitSuccess(10000);
    Async async = ctx.async(2);
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
    Arrays.asList("Julien", "Paulo").forEach(name -> {
      stub.sayHello(HelloRequest.newBuilder().setName(name).build(), new StreamObserver<HelloReply>() {
        private HelloReply result;
        @Override
        public void onNext(HelloReply helloReply) {
          result = helloReply;
        }
        @Override
        public void onError(Throwable throwable) {
          ctx.fail(throwable);
        }
        @Override
        public void onCompleted() {
          ctx.assertEquals("Hello " + name, result.getMessage());
          async.countDown();
        }
      });
    });
    async.awaitSuccess(10000);
    channel.shutdown();
  }

  @Test
  public void testBlockingException(TestContext ctx) {
    Metadata.Key<String> mdKey = Metadata.Key.of("mdkey", Metadata.ASCII_STRING_MARSHALLER);
    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        Metadata md = new Metadata();
        md.put(mdKey, "mdvalue");
        throw new StatusRuntimeException(Status.ABORTED, md);
      }
    };
    BindableService service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
        responseObserver.onCompleted();
      }
    };
    Async started = ctx.async();
    server = VertxServerBuilder.forPort(vertx, port)
      .addService(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .build()
      .start(ar -> {
        if (ar.succeeded()) {
          started.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
    stub.sayHello(HelloRequest.newBuilder().setName("Julien").build(), new StreamObserver<HelloReply>() {
      @Override
      public void onNext(HelloReply helloReply) {

      }
      @Override
      public void onError(Throwable throwable) {
        ctx.assertTrue(throwable instanceof StatusRuntimeException);
        StatusRuntimeException sre = (StatusRuntimeException) throwable;
        ctx.assertEquals(Status.ABORTED, sre.getStatus());
        ctx.assertEquals("mdvalue", sre.getTrailers().get(mdKey));
        async.countDown();
      }
      @Override
      public void onCompleted() {
        ctx.fail("StatusRuntimeException expected");
      }
    });
    async.awaitSuccess(10000);
    channel.shutdown();
  }

  @Test
  public void testStreamSource(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public void source(Empty request, StreamObserver<Item> stream) {
        int cnt = numItems;
        while (cnt-- > 0) {
          stream.onNext(Item.newBuilder().setValue("the-value-" + (numItems - cnt - 1)).build());
        }
        stream.onCompleted();
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);
    final List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build(), new StreamObserver<Item>() {
      @Override
      public void onNext(Item item) {
       items.add(item.getValue());
      }
      @Override
      public void onError(Throwable throwable) {
        ctx.fail(throwable);
      }
      @Override
      public void onCompleted() {
        List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
        ctx.assertEquals(expected, items);
        done.complete();
        channel.shutdown();
      }
    });
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> sink(StreamObserver<Empty> response) {
        List<String> items = new ArrayList<>();
        return new StreamObserver<Item>() {
          @Override
          public void onNext(Item item) {
            items.add(item.getValue());
          }
          @Override
          public void onError(Throwable throwable) {
            ctx.fail(throwable);
          }
          @Override
          public void onCompleted() {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            ctx.assertEquals(expected, items);
            done.complete();
            response.onNext(Empty.getDefaultInstance());
            response.onCompleted();
          }
        };
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);
    StreamObserver<Item> sink = stub.sink(new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty empty) {
      }
      @Override
      public void onError(Throwable throwable) {
        ctx.fail(throwable);
      }
      @Override
      public void onCompleted() {
      }
    });
    AtomicInteger count = new AtomicInteger(numItems);
    vertx.setPeriodic(10, id -> {
      int val = count.decrementAndGet();
      if (val >= 0) {
        sink.onNext(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
      } else {
        vertx.cancelTimer(id);
        sink.onCompleted();
        channel.shutdown();
      }
    });
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> pipe(StreamObserver<Item> responseObserver) {
        return new StreamObserver<Item>() {
          @Override
          public void onNext(Item item) {
            responseObserver.onNext(item);
          }
          @Override
          public void onError(Throwable throwable) {
            responseObserver.onError(throwable);
          }
          @Override
          public void onCompleted() {
            responseObserver.onCompleted();
          }
        };
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);
    final List<String> items = new ArrayList<>();
    StreamObserver<Item> pipe = stub.pipe(new StreamObserver<Item>() {
      @Override
      public void onNext(Item item) {
        items.add(item.getValue());
      }

      @Override
      public void onError(Throwable throwable) {
        ctx.fail(throwable);
      }

      @Override
      public void onCompleted() {
        List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
        ctx.assertEquals(expected, items);
        done.complete();
      }
    });
    AtomicInteger count = new AtomicInteger(numItems);
    vertx.setPeriodic(10, id -> {
      int val = count.decrementAndGet();
      if (val >= 0) {
        pipe.onNext(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
      } else {
        vertx.cancelTimer(id);
        pipe.onCompleted();
        channel.shutdown();
      }
    });
  }

  @Test
  public void testRandomPort(TestContext ctx) throws Exception {
    Async started = ctx.async();
    port = 0;
    startServer(new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
        responseObserver.onCompleted();
      }
    }, ar -> {
      if (ar.succeeded()) {
        started.complete();
      } else {
        ctx.fail(ar.cause());
      }
    });

    started.awaitSuccess(10000);

    ctx.assertTrue(server.getPort() > 0);
    ctx.assertTrue(server.getPort() < 65536);
  }
}
