package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.StreamObserver;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerCallResponse;
import io.vertx.grpc.server.GrpcStatus;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ServerTest extends GrpcTestBase2 {

  private static final int NUM_ITEMS = 128;
  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) {
    testUnary(should, "identity", "identity");
  }

  @Test
  public void testUnaryDecompression(TestContext should) {
    testUnary(should, "gzip", "identity");
  }

  @Test
  public void testUnaryCompression(TestContext should) {
    testUnary(should, "identity", "gzip");
  }

  private void testUnary(TestContext should, String requestEncoding, String responseEncoding) {

    startServer(new GrpcServer().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        GrpcServerCallResponse<HelloRequest, HelloReply> response = call.response();
        response
          .encoding(responseEncoding)
          .end(helloReply);
      });
    }));

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel).withCompression(requestEncoding);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    HelloReply res = stub.sayHello(request);
    should.assertEquals("Hello Julien", res.getMessage());
  }

  @Test
  public void testSSL(TestContext should) {

    startServer(new HttpServerOptions()
      .setSsl(true)
      .setUseAlpn(true)
      .setPort(8443)
      .setHost("localhost")
      .setKeyStoreOptions(new JksOptions()
        .setPath("tls/server-keystore.jks")
        .setPassword("wibble")), new GrpcServer().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        GrpcServerCallResponse<HelloRequest, HelloReply> response = call.response();
        response
          .end(helloReply);
      });
    }));

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", 8443)
      .useSsl(options -> {
        options.setSsl(true)
          .setUseAlpn(true)
          .setTrustStoreOptions(new JksOptions()
            .setPath("tls/client-truststore.jks")
            .setPassword("wibble"));
      })
      .build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    HelloReply res = stub.sayHello(request);
    should.assertEquals("Hello Julien", res.getMessage());
  }

  @Test
  public void testStatus(TestContext should) {

    startServer(new GrpcServer().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        GrpcServerCallResponse<HelloRequest, HelloReply> response = call.response();
        response
          .status(GrpcStatus.UNAVAILABLE)
          .end();
      });
    }));

    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
    try {
      stub.sayHello(request);
    } catch (StatusRuntimeException e) {
      should.assertEquals(Status.UNAVAILABLE, e.getStatus());
    }
  }

  @Test
  public void testServerStreaming(TestContext should) {

    startServer(new GrpcServer().callHandler(StreamingGrpc.getSourceMethod(), call -> {
      for (int i = 0; i < NUM_ITEMS; i++) {
        Item item = Item.newBuilder().setValue("the-value-" + i).build();
        call.response().write(item);
      }
      call.response().end();
    }));

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingBlockingStub stub = StreamingGrpc.newBlockingStub(channel);

    List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build()).forEachRemaining(item -> items.add(item.getValue()));
    List<String> expected = IntStream.rangeClosed(0, NUM_ITEMS - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
    should.assertEquals(expected, items);
  }

  @Test
  public void testClientStreaming(TestContext should) throws Exception {

    startServer(new GrpcServer().callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.handler(item -> {
        // Should assert item
      });
      call.endHandler(v -> {
        call.response().end(Empty.getDefaultInstance());
      });
    }));

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);

    Async test = should.async();
    StreamObserver<Item> items = stub.sink(new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
      }
      @Override
      public void onError(Throwable t) {
        should.fail(t);
      }
      @Override
      public void onCompleted() {
        test.complete();
      }
    });
    for (int i = 0; i < NUM_ITEMS; i++) {
      items.onNext(Item.newBuilder().setValue("the-value-" + i).build());
      Thread.sleep(10);
    }
    items.onCompleted();
  }

  @Test
  public void testBidiStreaming(TestContext should) throws Exception {

    startServer(new GrpcServer().callHandler(StreamingGrpc.getPipeMethod(), call -> {
      call.handler(item -> {
        call.response().write(item);
      });
      call.endHandler(v -> {
        call.response().end();
      });
    }));

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    StreamingGrpc.StreamingStub stub = StreamingGrpc.newStub(channel);

    Async test = should.async();
    List<String> items = new ArrayList<>();
    StreamObserver<Item> writer = stub.pipe(new StreamObserver<Item>() {
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
        test.complete();
      }
    });
    for (int i = 0; i < NUM_ITEMS; i++) {
      writer.onNext(Item.newBuilder().setValue("the-value-" + i).build());
      Thread.sleep(10);
    }
    writer.onCompleted();
    test.awaitSuccess(20_000);
    List<String> expected = IntStream.rangeClosed(0, NUM_ITEMS - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
    should.assertEquals(expected, items);
  }
}
