package io.vertx.grpc.impl;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Handler;
import io.vertx.grpc.GrpcWriteStream;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcWriteStreamImpl<T> implements GrpcWriteStream<T> {

  private final CallStreamObserver<T> observer;
  private final Handler<Throwable> errHandler;
  private Handler<Void> drainHandler;

  public GrpcWriteStreamImpl(StreamObserver<T> observer) {
    this.observer = (CallStreamObserver<T>) observer;
    this.errHandler = observer::onError;
    try {
      ((CallStreamObserver<T>) observer).setOnReadyHandler(this::drain);
    } catch (Exception e) {
      // Fails in some situation - for now we swallow it so the flow control can be implemented
    }
  }

  private void drain() {
    Handler<Void> handler = this.drainHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  @Override
  public GrpcWriteStreamImpl<T> exceptionHandler(Handler<Throwable> handler) {
    handler.handle(new RuntimeException("Unsupported Operation"));
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
  public GrpcWriteStreamImpl<T> setWriteQueueMaxSize(int i) {
    errHandler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return !observer.isReady();
  }

  @Override
  public GrpcWriteStreamImpl<T> drainHandler(Handler<Void> handler) {
    drainHandler = handler;
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
