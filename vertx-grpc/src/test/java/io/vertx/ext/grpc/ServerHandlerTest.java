package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.VertxStreamingGrpc;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.server.GrpcService;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ServerHandlerTest extends GrpcTestBase {

  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) throws Exception {

    Async test = should.async();

    VertxGreeterGrpc.GreeterVertxImplBase greeterVertxImplBase = new VertxGreeterGrpc.GreeterVertxImplBase() {
    };
    ServerServiceDefinition serviceDef = greeterVertxImplBase.bindService();
    ServerMethodDefinition<HelloRequest, HelloReply> meth = (ServerMethodDefinition<HelloRequest, HelloReply>) serviceDef.getMethod("helloworld.Greeter/SayHello");

    GrpcService service = new GrpcService();

    service.methodHandler(meth, call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        call.end(helloReply);
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(8080, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();
        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
        stub.sayHello(request).onComplete(should.asyncAssertSuccess(res -> {
          should.assertTrue(Context.isOnEventLoopThread());
          should.assertEquals("Hello Julien", res.getMessage());
          test.complete();
        }));
      }));
  }

  @Test
  public void testServerStreaming(TestContext should) throws Exception {

    int numItems = 128;

    Async test = should.async();

    VertxStreamingGrpc.StreamingVertxImplBase greeterVertxImplBase = new VertxStreamingGrpc.StreamingVertxImplBase() {
    };
    ServerServiceDefinition serviceDef = greeterVertxImplBase.bindService();
    ServerMethodDefinition<Empty, Item> method = (ServerMethodDefinition<Empty, Item>) serviceDef.getMethod("streaming.Streaming/Source");

    GrpcService service = new GrpcService();
    service.methodHandler(method, call -> {
      for (int i = 0;i < numItems;i++) {
        Item item = Item.newBuilder().setValue("the-value-" + i).build();
        call.write(item);
      }
      call.end();
    });

    vertx.createHttpServer().requestHandler(service).listen(8080, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        final List<String> items = new ArrayList<>();
        stub.source(Empty.newBuilder().build())
          .endHandler(v1 -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            test.complete();
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
      }));
  }

  @Test
  public void testClientStreaming(TestContext should) throws Exception {

    int numItems = 128;

    // Async test = should.async();

    VertxStreamingGrpc.StreamingVertxImplBase greeterVertxImplBase = new VertxStreamingGrpc.StreamingVertxImplBase() {
    };
    ServerServiceDefinition serviceDef = greeterVertxImplBase.bindService();
    ServerMethodDefinition<Item, Empty> method = (ServerMethodDefinition<Item, Empty>) serviceDef.getMethod("streaming.Streaming/Sink");

    GrpcService service = new GrpcService();
    service.methodHandler(method, call -> {
      call.handler(item -> {
        // Should assert item
      });
      call.endHandler(v -> {
        call.end(Empty.getDefaultInstance());
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(8080, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        AtomicInteger count = new AtomicInteger(numItems);

        Handler<WriteStream<Item>> h = ws -> vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            ws.end();
          }
        });

        stub.sink(h)
          .onComplete(should.asyncAssertSuccess());
      }));
  }

  @Test
  public void testBidiStreaming(TestContext should) throws Exception {

    int numItems = 128;

    Async test = should.async();

    VertxStreamingGrpc.StreamingVertxImplBase greeterVertxImplBase = new VertxStreamingGrpc.StreamingVertxImplBase() {
    };
    ServerServiceDefinition serviceDef = greeterVertxImplBase.bindService();
    ServerMethodDefinition<Item, Item> def = (ServerMethodDefinition<Item, Item>) serviceDef.getMethod("streaming.Streaming/Pipe");

    GrpcService service = new GrpcService();
    service.methodHandler(def, call -> {
      call.handler(item -> {
        call.write(item);
      });
      call.endHandler(v -> {
        call.end();
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(8080, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();
        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        final List<String> items = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(numItems);

        Handler<WriteStream<Item>> h = ws -> vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            ws.end();
          }
        });

        stub.pipe(h)
          .endHandler(v1 -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            test.complete();
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
      }));
  }
}
