package io.vertx.ext.grpc.utils;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

public abstract class BaseReadStream<T> implements ReadStream<T> {

  protected Handler<Throwable> exceptionHandler;

  protected Handler<Void> endHandler;

  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;

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
  public ReadStream<T> endHandler(Handler<Void> handler) {
    this.endHandler = handler;

    return this;
  }

}
