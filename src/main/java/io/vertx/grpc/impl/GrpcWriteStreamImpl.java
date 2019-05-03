package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.GrpcWriteStream;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcWriteStreamImpl<T> implements GrpcWriteStream<T> {

  private final StreamObserver<T> observer;
  private final Handler<Throwable> errHandler;

  public GrpcWriteStreamImpl(StreamObserver<T> observer) {
    this.observer = observer;
    this.errHandler = observer::onError;
  }

  @Override
  public GrpcWriteStreamImpl<T> exceptionHandler(Handler<Throwable> handler) {
    handler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

  @Override
  public WriteStream<T> write(T data, Handler<AsyncResult<Void>> handler) {
    observer.onNext(data);
    handler.handle(Future.succeededFuture());
    return this;
  }

  @Override
  public GrpcWriteStreamImpl<T> write(T t) {
    observer.onNext(t);
    return this;
  }

  @Override
  public void end() {
    observer.onCompleted();
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    observer.onCompleted();
    handler.handle(Future.succeededFuture());
  }

  @Override
  public GrpcWriteStreamImpl<T> setWriteQueueMaxSize(int i) {
    errHandler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public GrpcWriteStreamImpl<T> drainHandler(Handler<Void> handler) {
    errHandler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

  public GrpcWriteStreamImpl<T> fail(Throwable t) {
    observer.onError(t);
    return this;
  }

  public StreamObserver<T> writeObserver() {
    return observer;
  }
}
