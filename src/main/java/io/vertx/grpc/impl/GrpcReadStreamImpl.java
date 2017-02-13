package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Handler;
import io.vertx.grpc.GrpcReadStream;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcReadStreamImpl<T> implements GrpcReadStream<T> {

  private Handler<T> streamHandler;
  private Handler<Throwable> errorHandler;
  private Handler<Void> endHandler;
  private StreamObserver<T> observer;

  public GrpcReadStreamImpl(StreamObserver<T> observer) {
    this.observer = observer;
  }

  public GrpcReadStreamImpl() {

  }

  @Override
  public GrpcReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  @Override
  public GrpcReadStream<T> handler(Handler<T> handler) {
    this.streamHandler = handler;
    return this;
  }

  @Override
  public GrpcReadStream<T> pause() {
    return this;
  }

  @Override
  public GrpcReadStream<T> resume() {
    return this;
  }

  @Override
  public GrpcReadStream<T> endHandler(Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public StreamObserver<T> readObserver() {
    if (observer == null) {
      observer = new StreamObserver<T>() {
        @Override
        public void onNext(T value) {
          streamHandler.handle(value);
        }

        @Override
        public void onError(Throwable t) {
          errorHandler.handle(t);
        }

        @Override
        public void onCompleted() {
          endHandler.handle(null);
        }
      };
    }
    return observer;
  }

  @Override
  public GrpcReadStream<T> setReadObserver(StreamObserver<T> observer) {
    this.observer = observer;
    return this;
  }
}
