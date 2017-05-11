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
  private final boolean internal;
  private final Handler<Throwable> errHandler;
  private Handler<Void> drainHandler;

  public GrpcWriteStreamImpl(StreamObserver<T> observer, boolean internal) {
    this.observer = (CallStreamObserver<T>) observer;
    this.internal = internal;
    this.errHandler = observer::onError;
    if (!internal) {
      this.observer.setOnReadyHandler(this::drain);
    }
  }

  private void drain() {
    if (drainHandler != null) {
      drainHandler.handle(null);
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
    if (internal) {
      throw new IllegalStateException("This is an internally managed stream, drainHandler is not allowed");
    }
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
