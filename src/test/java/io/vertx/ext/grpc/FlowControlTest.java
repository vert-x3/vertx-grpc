package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcWriteStream;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowControlTest extends GrpcTestBase {

  @Test
  public void testStreamSource(TestContext ctx) throws Exception {
    Async done = ctx.async();
    CompletableFuture<GrpcWriteStream<Item>> writeStreamFut = new CompletableFuture<>();
    CompletableFuture<GrpcReadStream<Item>> readStreamFut = new CompletableFuture<>();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, GrpcWriteStream<Item> stream) {
        writeStreamFut.complete(stream);
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    stub.source(Empty.newBuilder().build(), readStreamFut::complete);
    GrpcWriteStream<Item> writeStream = writeStreamFut.get(10, TimeUnit.SECONDS);
    GrpcReadStream<Item> readStream = readStreamFut.get(10, TimeUnit.SECONDS);
    sendBatch(100, readStream, writeStream, done);
  }

  // Does not pass

  // java.lang.IllegalStateException: Cannot alter onReadyHandler after call started

  // uncomment the try/catch in GrpcWriteStreamImpl constructor

  // at io.grpc.stub.ClientCalls$CallToStreamObserverAdapter.setOnReadyHandler(ClientCalls.java:317)
  // at io.vertx.grpc.impl.GrpcWriteStreamImpl.<init>(GrpcWriteStreamImpl.java:20)
  // at io.vertx.grpc.GrpcWriteStream.create(GrpcWriteStream.java:20)
  // at io.vertx.grpc.impl.GrpcUniExchangeImpl.<init>(GrpcUniExchangeImpl.java:24)
  // at io.vertx.grpc.GrpcUniExchange.create(GrpcUniExchange.java:19)
  // at io.grpc.examples.streaming.StreamingGrpc$StreamingVertxStub.sink(StreamingGrpc.java:368)
  // at io.vertx.ext.grpc.FlowControlTest.testStreamSink(FlowControlTest.java:61)
  // at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  // at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
  // at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  // at java.lang.reflect.Method.invoke(Method.java:498)

//  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    Async done = ctx.async();
    CompletableFuture<GrpcWriteStream<Item>> writeStreamFut = new CompletableFuture<>();
    CompletableFuture<GrpcReadStream<Item>> readStreamFut = new CompletableFuture<>();
    startServer(new StreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void sink(GrpcReadStream<Item> request, Future<Empty> response) {
        readStreamFut.complete(request);
      }
    });
    ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();
    StreamingGrpc.StreamingVertxStub stub = StreamingGrpc.newVertxStub(channel);
    stub.sink(writeStreamFut::complete);
    GrpcWriteStream<Item> writeStream = writeStreamFut.get(10, TimeUnit.SECONDS);
    GrpcReadStream<Item> readStream = readStreamFut.get(10, TimeUnit.SECONDS);
    sendBatch(100, readStream, writeStream, done);
  }

  private void sendBatch(int times, GrpcReadStream<Item> readStream, GrpcWriteStream<Item> writeStream, Async done) {
    if (times > 0) {
      readStream.pause();
      long num = 0;
      while (!writeStream.writeQueueFull()) {
        writeStream.write(Item.newBuilder().setValue("the-value-" + num++).build());
      }
      AtomicLong val = new AtomicLong(num);
      readStream.handler(item -> {
        if (val.decrementAndGet() == 0) {
          sendBatch(times - 1, readStream, writeStream, done);
        }
      });
      readStream.resume();
    } else {
      readStream.endHandler(v -> done.complete());
      writeStream.end();
    }
  }
}
