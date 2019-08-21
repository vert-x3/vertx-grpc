package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.VertxStreamingGrpc;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.*;
import org.junit.Test;

import io.grpc.examples.helloworld.*;
import io.vertx.core.streams.WriteStream;

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
    serverCtx.runOnContext(v -> startServer(new VertxGreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, Promise<HelloReply> response) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());

        response.complete(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
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
      VertxGreeterGrpc.VertxGreeterStub stub = VertxGreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request).setHandler(res -> {
        if (res.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", res.result().getMessage());
          async.complete();
          channel.shutdown();
        } else {
          ctx.fail(res.cause());
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
    BindableService service = new VertxGreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, Promise<HelloReply> response) {

        ctx.assertTrue(Context.isOnEventLoopThread());
        response.complete(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
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
    VertxGreeterGrpc.VertxGreeterStub stub = VertxGreeterGrpc.newVertxStub(channel);
    Arrays.asList("Julien", "Paulo").forEach(name -> {
      stub.sayHello(HelloRequest.newBuilder().setName(name).build()).setHandler(res -> {
        if (res.succeeded()) {
          ctx.assertEquals("Hello " + name, res.result().getMessage());
          async.countDown();
        } else {
          ctx.fail(res.cause());
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
    BindableService service = new VertxGreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, Promise<HelloReply> response) {
        response.complete(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
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
    VertxGreeterGrpc.VertxGreeterStub stub = VertxGreeterGrpc.newVertxStub(channel);
    stub.sayHello(HelloRequest.newBuilder().setName("Julien").build()).setHandler(res -> {
      if (res.succeeded()) {
        ctx.fail("StatusRuntimeException expected");
      } else {
        Throwable throwable = res.cause();
        ctx.assertTrue(throwable instanceof StatusRuntimeException);
        StatusRuntimeException sre = (StatusRuntimeException) throwable;
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
    startServer(new VertxStreamingGrpc.StreamingImplBase() {
      @Override
      public void source(Empty request, WriteStream<Item> response) {
        new IterableReadStream<>(cnt -> Item.newBuilder().setValue("the-value-" + cnt).build(), numItems).pipeTo(response);
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext(true)
     .build();
    VertxStreamingGrpc.VertxStreamingStub stub = VertxStreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build())
     .endHandler(v -> {
       List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
       ctx.assertEquals(expected, items);
       done.complete();
       channel.shutdown();
     })
     .exceptionHandler(ctx::fail)
     .handler(item -> items.add(item.getValue()));
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new VertxStreamingGrpc.StreamingImplBase() {
      @Override
      public void sink(ReadStream<Item> request, Promise<Empty> response) {
        List<String> items = new ArrayList<>();

        request
         .endHandler(v -> {
           List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
           ctx.assertEquals(expected, items);
           done.complete();
           response.complete(Empty.getDefaultInstance());
         })
         .exceptionHandler(ctx::fail)
         .handler(item -> items.add(item.getValue()));
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext(true)
     .build();
    VertxStreamingGrpc.VertxStreamingStub stub = VertxStreamingGrpc.newVertxStub(channel);
    AtomicInteger count = new AtomicInteger(numItems);

    Handler<WriteStream<Item>> h = ws -> {
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          ws.end();
          channel.shutdown();
        }
      });

    };

    stub.sink(h)
     .setHandler(res -> {
       if (res.failed()) {
         ctx.fail();
       }
     });
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new VertxStreamingGrpc.StreamingImplBase() {
      @Override
      public void pipe(ReadStream<Item> request, WriteStream<Item> response) {
        request.pipeTo(response);
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext(true)
     .build();
    VertxStreamingGrpc.VertxStreamingStub stub = VertxStreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    AtomicInteger count = new AtomicInteger(numItems);

    Handler<WriteStream<Item>> h = ws -> {
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          ws.end();
          channel.shutdown();
        }
      });
    };

    stub.pipe(h)
     .endHandler(v -> {
       List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
       ctx.assertEquals(expected, items);
       done.complete();
     })
     .exceptionHandler(ctx::fail)
     .handler(item -> items.add(item.getValue()));

  }

  @Test
  public void testRandomPort(TestContext ctx) throws Exception {
    Async started = ctx.async();
    port = 0;
    startServer(new VertxGreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, Promise<HelloReply> response) {
        response.complete(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
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
