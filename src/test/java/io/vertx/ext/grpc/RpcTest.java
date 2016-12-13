package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.StreamHelper;
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
    serverCtx.runOnContext(v -> {
      startServer(new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
          ctx.assertEquals(serverCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
          responseObserver.onNext(reply);
          responseObserver.onCompleted();
        }
      }, ar -> {
        if (ar.succeeded()) {
          started.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext(true)
          .build();
      GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, StreamHelper.future(ar -> {
        if (ar.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", ar.result().getMessage());
          async.complete();
        } else {
          ctx.fail(ar.cause());
        }
      }));
    });
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public StreamObserver<Item> sink(StreamObserver<Empty> responseObserver) {
        List<String> items = new ArrayList<>();
        return new StreamObserver<Item>() {
          @Override
          public void onNext(Item value) {
            items.add(value.getValue());
          }
          @Override
          public void onError(Throwable t) {
            ctx.fail("Unexpected error");
          }
          @Override
          public void onCompleted() {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(v -> "the-value-" + v).collect(Collectors.toList());
            ctx.assertEquals(expected, items);
            done.complete();
            responseObserver.onCompleted();
          }
        };
      }
    });
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);
    StreamObserver<Item> sink = stub.sink(new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
        ctx.fail();
      }
      @Override
      public void onError(Throwable t) {
        ctx.fail();
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
      }
    });
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async started = ctx.async();
    Async done = ctx.async();
    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public ServerServiceDefinition bindService() {
        ServerServiceDefinition sd = super.bindService();
        started.complete();
        return sd;
      }
      @Override
      public StreamObserver<Item> pipe(StreamObserver<Item> responseObserver) {
        return responseObserver;
      }
    });
    started.awaitSuccess(10000);
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);
    StreamObserver<Item> sink = stub.pipe(new StreamObserver<Item>() {
      List<String> items = new ArrayList<>();
      @Override
      public void onNext(Item value) {
        items.add(value.getValue());
      }
      @Override
      public void onError(Throwable t) {
        ctx.fail("Unexpected error");
      }
      @Override
      public void onCompleted() {
        List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(v -> "the-value-" + v).collect(Collectors.toList());
        ctx.assertEquals(expected, items);
        done.complete();
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
      }
    });
  }
}
