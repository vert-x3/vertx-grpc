package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcUniExchange;
import io.vertx.grpc.GrpcWriteStream;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcUniExchangeImpl<O,I> implements GrpcUniExchange<O,I> {

  private final GrpcWriteStream<O> writeStream;
  private final AtomicBoolean complete = new AtomicBoolean();

  private Handler<AsyncResult<I>> handler;

  public GrpcUniExchangeImpl(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    this.writeStream = GrpcWriteStream.create(writeObserver);

    readStream.endHandler(v -> {
      if (complete.compareAndSet(false, true)) {
        if (GrpcUniExchangeImpl.this.handler != null) {
          GrpcUniExchangeImpl.this.handler.handle(Future.succeededFuture());
        }
      }
    });

    readStream.handler(input -> {
      if (complete.compareAndSet(false, true)) {
        if (GrpcUniExchangeImpl.this.handler != null) {
          GrpcUniExchangeImpl.this.handler.handle(Future.succeededFuture(input));
        }
      }
    });

    readStream.exceptionHandler(t -> {
      if (complete.compareAndSet(false, true)) {
        if (GrpcUniExchangeImpl.this.handler != null) {
          GrpcUniExchangeImpl.this.handler.handle(Future.failedFuture(t));
        }
      }
    });
  }

  @Override
  public GrpcUniExchange<O, I> exceptionHandler(Handler<Throwable> handler) {
    throw new RuntimeException("Unsupported Operation");
  }

  @Override
  public GrpcUniExchange<O, I> handler(Handler<AsyncResult<I>> handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public GrpcUniExchange<O, I> write(O data) {
    writeStream.write(data);
    return this;
  }

  @Override
  public WriteStream<O> write(O data, Handler<AsyncResult<Void>> handler) {
    writeStream.write(data, handler);
    return this;
  }

  @Override
  public void end() {
    writeStream.end();
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    writeStream.end(handler);
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
