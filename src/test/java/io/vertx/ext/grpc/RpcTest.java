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
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class RpcTest extends GrpcTestBase {

  @Test
  public void testSimple(TestContext ctx) throws Exception {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v -> startServer(new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Future<HelloReply> future) {
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
      });
    });
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
    stub.source(Empty.newBuilder().build(), GrpcReadStream.<Item>create()
      .exceptionHandler(ctx::fail)
      .handler(item -> items.add(item.getValue()))
      .endHandler(v -> {
        List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
        ctx.assertEquals(expected, items);
        done.complete();
      }));
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public GrpcReadStream<Item> sink(Future<Empty> responseObserver) {
        List<String> items = new ArrayList<>();
        return GrpcReadStream.<Item>create()
          .exceptionHandler(ctx::fail)
          .handler(item -> items.add(item.getValue()))
          .endHandler(v -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            ctx.assertEquals(expected, items);
            done.complete();
            responseObserver.complete();
          });
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
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
      public GrpcReadStream<Item> pipe(GrpcWriteStream<Item> responseObserver) {
        return GrpcReadStream.<Item>create()
          .handler(responseObserver::write)
          .exceptionHandler(responseObserver::fail)
          .endHandler(v -> responseObserver.end());
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
        }
      });
    });
  }
}
