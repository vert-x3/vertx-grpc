package io.vertx.grpc.client;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.common.GrpcStatus;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TypedApiTest extends GrpcTestBase {

  private static final int NUM_ITEMS = 128;

  @Test
  public void testUnary(TestContext should) throws IOException {

    GreeterGrpc.GreeterImplBase called = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> plainResponseObserver) {
        ServerCallStreamObserver<HelloReply> responseObserver =
          (ServerCallStreamObserver<HelloReply>) plainResponseObserver;
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };
    startServer(called);

    Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    GrpcUnaryCall<HelloRequest, HelloReply> call = client.unaryCall(GreeterGrpc.getSayHelloMethod());
    call.call(SocketAddress.inetSocketAddress(port, "localhost"), HelloRequest.newBuilder().setName("Julien").build())
      .onComplete(should.asyncAssertSuccess(callRequest -> {
        should.assertEquals("Hello Julien", callRequest.getMessage());
        test.complete();
      }));
  }

  @Test
  public void testServerStreaming(TestContext should) throws IOException {

    startServer(new StreamingGrpc.StreamingImplBase() {
      @Override
      public void source(Empty request, StreamObserver<Item> responseObserver) {
        for (int i = 0;i < NUM_ITEMS;i++) {
          responseObserver.onNext(Item.newBuilder().setValue("the-value-" + i).build());
        }
        responseObserver.onCompleted();
      }
    });

    final Async test = should.async();
    GrpcClient client = GrpcClient.client(vertx);
    GrpcServerStreamingCall<Empty, Item> call = client.serverStreamingCall(StreamingGrpc.getSourceMethod());

    Future<ReadStream<Item>> fut = call.call(SocketAddress.inetSocketAddress(port, "localhost"), Empty.getDefaultInstance());
    fut.onComplete(should.asyncAssertSuccess(stream -> {
      List<Item> items = new ArrayList<>();
      stream.handler(items::add);
      stream.endHandler(v -> {
        should.assertEquals(NUM_ITEMS, items.size());
        test.complete();
      });
    }));
  }
}
