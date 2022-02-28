package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class FlowControlTest extends GrpcTestBase {

  @Test
  public void testServerStreaming(TestContext should) throws Exception {

    Async done = should.async();

    AtomicInteger count = new AtomicInteger();
    CompletableFuture<Integer> latch = new CompletableFuture<>();

    GrpcServer service = new GrpcServer();
    service.callHandler(StreamingGrpc.getSourceMethod(), callRequest -> {
      Promise<Void> p = Promise.promise();
      send(() -> Item.newBuilder().setValue("the-value-" + count.getAndIncrement()).build(), callRequest.response(), p);
      p.future().onComplete(should.asyncAssertSuccess(v2 -> {
        latch.complete(count.get());
        callRequest.response().end();
      }));
    });
    vertx.createHttpServer().requestHandler(service).listen(port, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSourceMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              callResponse.pause();
              AtomicInteger received = new AtomicInteger();
              callResponse.handler(item -> {
                should.assertTrue(latch.isDone());
                should.assertEquals("the-value-" + received.getAndIncrement(), item.getValue());
              });
              callResponse.endHandler(v2 -> {
                should.assertTrue(latch.isDone());
                should.assertEquals(received.get(), count.get());
                done.complete();
              });
              latch.whenComplete((v2, err) -> {
                callResponse.resume();
              });
            }));
            callRequest.end(Empty.getDefaultInstance());
          }));
      }));
  }

  @Test
  public void testClientStreaming(TestContext should) throws Exception {

    Async done = should.async();

    AtomicInteger count = new AtomicInteger();
    CompletableFuture<Integer> latch = new CompletableFuture<>();

    GrpcServer service = new GrpcServer();
    service.callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.pause();
      AtomicInteger received = new AtomicInteger();
      call.handler(item -> {
        should.assertTrue(latch.isDone());
        should.assertEquals("the-value-" + received.getAndIncrement(), item.getValue());
      });
      call.endHandler(v -> {
        should.assertTrue(latch.isDone());
        should.assertEquals(received.get(), count.get());
        call.response().end(Empty.getDefaultInstance());
      });
      latch.whenComplete((v, err) -> {
        call.resume();
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(port, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSinkMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              callResponse.messageHandler(item -> {
                count.incrementAndGet();
              });
              callResponse.endHandler(v2 -> {
                done.complete();
              });
            }));
            Promise<Void> p = Promise.promise();
            send(() -> Item.newBuilder().setValue("the-value-" + count.getAndIncrement()).build(), callRequest, p);
            p.future().onComplete(should.asyncAssertSuccess(v2 -> {
              latch.complete(count.get());
              callRequest.end();
            }));
          }));
      }));
  }

  private <I> void send(Supplier<I> supplier, WriteStream<I> stream, Promise<Void> p) {
    while (!stream.writeQueueFull()) {
      I item = supplier.get();
      stream.write(item);
    }
    Vertx.currentContext().owner().setTimer(100, id -> {
      if (stream.writeQueueFull()) {
        p.complete();
      } else {
        send(supplier, stream, p);
      }
    });
  }

}
