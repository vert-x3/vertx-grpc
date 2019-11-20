package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.*;
import org.junit.Test;

import io.grpc.examples.helloworld.*;

import java.util.ArrayList;
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
    serverCtx.runOnContext(v -> startServer(new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Promise<HelloReply> future) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());
        future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
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
      GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, ar -> {
        if (ar.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", ar.result().getMessage());
          async.complete();
        } else {
          ctx.fail(ar.cause());
        }
        channel.shutdown();
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
    BindableService service = new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Promise<HelloReply> future) {
        ctx.assertTrue(Context.isOnEventLoopThread());
        future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
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
    GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
    stub.sayHello(HelloRequest.newBuilder().setName("Julien").build(), ar -> {
      if (ar.succeeded()) {
        ctx.assertEquals("Hello Julien", ar.result().getMessage());
        async.countDown();
      } else {
        ctx.fail(ar.cause());
      }
    });
    stub.sayHello(HelloRequest.newBuilder().setName("Paulo").build(), ar -> {
      if (ar.succeeded()) {
        ctx.assertEquals("Hello Paulo", ar.result().getMessage());
        async.countDown();
      } else {
        ctx.fail(ar.cause());
      }
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
    BindableService service = new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Promise<HelloReply> future) {
        future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
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
    GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
    stub.sayHello(HelloRequest.newBuilder().setName("Julien").build(), ar -> {
      if (ar.succeeded()) {
        ctx.fail("StatusRuntimeException expected");
      } else {
        ctx.assertTrue(ar.cause() instanceof StatusRuntimeException);
        StatusRuntimeException sre = (StatusRuntimeException) ar.cause();
        ctx.assertEquals(Status.ABORTED, sre.getStatus());
        ctx.assertEquals("mdvalue", sre.getTrailers().get(mdKey));
        async.countDown();
      }
    });
    async.awaitSuccess(10000);
    channel.shutdown();
  }

  @Test
  public void testStreamSource(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, GrpcWriteStream<Item> stream) {
        int cnt = numItems;
        while (cnt-- > 0) {
          stream.write(Item.newBuilder().setValue("the-value-" + (numItems - cnt - 1)).build());
        }
        stream.end();
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build(), exchange -> {
      exchange
        .exceptionHandler(ctx::fail)
        .handler(item -> items.add(item.getValue()))
        .endHandler(v -> {
          List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
          ctx.assertEquals(expected, items);
          done.complete();
          channel.shutdown();
        });
    });
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void sink(GrpcReadStream<Item> request, Future<Empty> response) {
        List<String> items = new ArrayList<>();
        request
          .exceptionHandler(ctx::fail)
          .handler(item -> items.add(item.getValue()))
          .endHandler(v -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            ctx.assertEquals(expected, items);
            done.complete();
            response.complete();
          });
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    stub.sink(exchange -> {
      exchange
        .handler(ar -> {
          if (ar.failed()) {
            ctx.fail(ar.cause());
          }
        });

      AtomicInteger count = new AtomicInteger(numItems);
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          exchange.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          exchange.end();
          channel.shutdown();
        }
      });
    });
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void pipe(GrpcBidiExchange<Item, Item> exchange) {
        exchange
          .handler(exchange::write)
          .exceptionHandler(exchange::fail)
          .endHandler(v -> exchange.end());
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    stub.pipe(exchange -> {
      exchange
        .exceptionHandler(ctx::fail)
        .handler(item -> items.add(item.getValue()))
        .endHandler(V -> {
          List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
          ctx.assertEquals(expected, items);
          done.complete();
        });

      AtomicInteger count = new AtomicInteger(numItems);
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          exchange.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          exchange.end();
          channel.shutdown();
        }
      });
    });
  }

  @Test
  public void testRandomPort(TestContext ctx) throws Exception {
    Async started = ctx.async();
    port = 0;
    startServer(new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Promise<HelloReply> future) {
        future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
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
