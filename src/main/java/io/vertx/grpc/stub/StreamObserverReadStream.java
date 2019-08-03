package io.vertx.grpc.stub;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

/**
 * @author Rogelio Orts
 */
class StreamObserverReadStream<T> implements StreamObserver<T>, ReadStream<T> {

  private Handler<Throwable> exceptionHandler;

  private Handler<T> handler;

  private Handler<Void> endHandler;

  @Override
  public void onNext(T t) {
    if (handler != null) {
      handler.handle(t);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    if (exceptionHandler != null) {
      exceptionHandler.handle(throwable);
    }
  }

  @Override
  public void onCompleted() {
    if (endHandler != null) {
      endHandler.handle(null);
    }
  }

  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;

    return this;
  }

  @Override
  public ReadStream<T> handler(@Nullable Handler<T> handler) {
    this.handler = handler;

    return this;
  }

  @Override
  public ReadStream<T> pause() {
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    return this;
  }

  @Override
  public ReadStream<T> fetch(long l) {
    return this;
  }

  @Override
  public ReadStream<T> endHandler(@Nullable Handler<Void> handler) {
    this.endHandler = handler;

    return this;
  }

}
