package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.vertx.core.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcWriteStream;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.Test;

import io.grpc.examples.helloworld.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class RpcTest extends GrpcTestBase {

  @Test
  public void testSimple(TestContext ctx) throws Exception {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v -> startServer(new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Handler<AsyncResult<HelloReply>> handler) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        handler.handle(Future.succeededFuture(reply));
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
      ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
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
      });
    });
  }

  @Test
  public void testStreamSource(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, Handler<AsyncResult<Item>> responseObserver) {
        int cnt = numItems;
        while(cnt-- > 0) {
          responseObserver.handle(Future.succeededFuture(Item.newBuilder().setValue("the-value-" + (numItems - cnt - 1)).build()));
        }
        responseObserver.handle(Future.succeededFuture());
      }
    });
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
            .usePlaintext(true)
            .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build(), ar -> {
      if (ar.failed()) {
        ctx.fail(ar.cause());
      } else {
        final Item item = ar.result();
        if (item != null) {
          items.add(item.getValue());
        } else {
          List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(v -> "the-value-" + v).collect(Collectors.toList());
          ctx.assertEquals(expected, items);
          done.complete();
        }
      }
    });
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public GrpcReadStream<Item> sink(Handler<AsyncResult<Empty>> responseObserver) {
        List<String> items = new ArrayList<>();
        return GrpcReadStream.<Item>create()
                .exceptionHandler(ctx::fail)
                .handler(item -> items.add(item.getValue()))
                .endHandler(v -> {
                  List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
                  ctx.assertEquals(expected, items);
                  done.complete();
                  responseObserver.handle(Future.succeededFuture());
                });
      }
    });
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    GrpcWriteStream<Item> sink = stub.sink(ar -> {
      if (ar.failed()) {
        ctx.fail(ar.cause());
      }
    });
    AtomicInteger count = new AtomicInteger(numItems);
    vertx.setPeriodic(10, id -> {
      int val = count.decrementAndGet();
      if (val >= 0) {
        sink.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
      } else {
        vertx.cancelTimer(id);
        sink.end();
      }
    });
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public GrpcReadStream<Item> pipe(Handler<AsyncResult<Item>> responseObserver) {
        return GrpcReadStream.<Item>create()
                .handler(item -> responseObserver.handle(Future.succeededFuture(item)))
                .exceptionHandler(t -> responseObserver.handle(Future.failedFuture(t)))
                .endHandler(v -> responseObserver.handle(Future.succeededFuture()));
      }
    });
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    GrpcWriteStream<Item> pipe = stub.pipe(ar -> {
      if (ar.succeeded()) {
        final Item value = ar.result();
        if (value != null) {
          items.add(value.getValue());
        } else {
          List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(v -> "the-value-" + v).collect(Collectors.toList());
          ctx.assertEquals(expected, items);
          done.complete();
        }
      } else {
        ctx.fail(ar.cause());
      }
    });

    AtomicInteger count = new AtomicInteger(numItems);
    vertx.setPeriodic(10, id -> {
      int val = count.decrementAndGet();
      if (val >= 0) {
        pipe.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
      } else {
        vertx.cancelTimer(id);
        pipe.end();
      }
    });
  }
}
