package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Handler;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcUniExchange;
import io.vertx.grpc.GrpcWriteStream;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcUniExchangeImpl<O,I> implements GrpcUniExchange<O,I> {

  private final GrpcWriteStream<O> writeStream;
  private final GrpcReadStream<I> readStream;
  private final AtomicBoolean complete = new AtomicBoolean();

  private Handler<I> endHandler;

  public GrpcUniExchangeImpl(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    this.readStream = readStream;
    this.writeStream = GrpcWriteStream.create(writeObserver);

    this.readStream.endHandler(v -> {
      if (!complete.compareAndSet(false, true)) {
        throw new IllegalStateException("Result is already complete");
      }
    });

    this.readStream.handler(input -> {
      if (complete.get()) {
        throw new IllegalStateException("Result is already complete");
      }
      if (GrpcUniExchangeImpl.this.endHandler != null) {
        GrpcUniExchangeImpl.this.endHandler.handle(input);
      }
    });
  }

  @Override
  public GrpcUniExchange<O, I> exceptionHandler(Handler<Throwable> handler) {
    readStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcUniExchange<O, I> endHandler(Handler<I> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public GrpcUniExchange<O, I> write(O data) {
    writeStream.write(data);
    return this;
  }

  @Override
  public void end() {
    writeStream.end();
  }

  @Override
  public GrpcUniExchange<O, I> setWriteQueueMaxSize(int maxSize) {
    writeStream.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return writeStream.writeQueueFull();
  }

  @Override
  public GrpcUniExchange<O, I> drainHandler(Handler<Void> handler) {
    writeStream.drainHandler(handler);
    return this;
  }

  @Override
  public GrpcUniExchange<O, I> fail(Throwable t) {
    writeStream.fail(t);
    return this;
  }

  @Override
  public StreamObserver<O> writeObserver() {
    return writeStream.writeObserver();
  }
}
