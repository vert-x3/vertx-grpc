package io.vertx.ext.grpc;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.examples.streaming.VertxStreamingGrpc;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcClient;
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
public class ClientTest extends GrpcTestBase {

  @Override
  public void tearDown(TestContext should) {
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) {

    Async test = should.async();

    startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        should.assertTrue(Context.isOnEventLoopThread());
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), GreeterGrpc.getSayHelloMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              AtomicInteger count = new AtomicInteger();
              callResponse.handler(reply -> {
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
      });
  }

  @Test
  public void testServerStreaming(TestContext should) {
    int numItems = 128;
    final Async test = should.async();

    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, WriteStream<Item> response) {
        new IterableReadStream<>(cnt -> Item.newBuilder().setValue("the-value-" + cnt).build(), numItems).pipeTo(response);
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSourceMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              AtomicInteger count = new AtomicInteger();
              callResponse.handler(item -> {
                int i = count.getAndIncrement();
                should.assertEquals("the-value-" + i, item.getValue());
              });
              callResponse.endHandler(v2 -> {
                should.assertEquals(numItems, count.get());
                test.complete();
              });
            }));
            callRequest.end(Empty.getDefaultInstance());
          }));
      });
  }

  @Test
  public void testClientStreaming(TestContext should) {
    int numItems = 128;
    Async done = should.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public Future<Empty> sink(ReadStream<Item> request) {
        List<String> items = new ArrayList<>();
        Promise<Empty> promise = Promise.promise();
        request
          .endHandler(v -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            promise.complete(Empty.getDefaultInstance());
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
        return promise.future();
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSinkMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              AtomicInteger count = new AtomicInteger();
              callResponse.handler(item -> {
                count.incrementAndGet();
              });
              callResponse.endHandler(v2 -> {
                should.assertEquals(1, count.get());
                done.complete();
              });
            }));
            AtomicInteger count = new AtomicInteger(numItems);
            vertx.setPeriodic(10, id -> {
              int val = count.decrementAndGet();
              if (val >= 0) {
                callRequest.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
              } else {
                vertx.cancelTimer(id);
                callRequest.end();
              }
            });
          }));
      });
  }

  @Test
  public void testBidiStreaming(TestContext should) {
    int numItems = 128;
    Async done = should.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void pipe(ReadStream<Item> request, WriteStream<Item> response) {
        request.pipeTo(response);
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getPipeMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              AtomicInteger count = new AtomicInteger();
              callResponse.handler(item -> {
                int i = count.getAndIncrement();
                should.assertEquals("the-value-" + i, item.getValue());
              });
              callResponse.endHandler(v2 -> {
                should.assertEquals(numItems, count.get());
                done.complete();
              });
            }));
            AtomicInteger count = new AtomicInteger(numItems);
            vertx.setPeriodic(10, id -> {
              int val = count.decrementAndGet();
              if (val >= 0) {
                callRequest.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
              } else {
                vertx.cancelTimer(id);
                callRequest.end();
              }
            });
          }));
      });
  }
}
