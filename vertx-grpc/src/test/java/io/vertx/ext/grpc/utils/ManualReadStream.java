package io.vertx.ext.grpc.utils;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

public class ManualReadStream<T> extends BaseReadStream<T> {

  private Handler<T> handler;

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.handler = handler;

    return this;
  }

  public void end() {
    if (endHandler != null) {
      endHandler.handle(null);
    }
  }

  public void send(T value) {
    if (handler != null) {
      handler.handle(value);
    }
  }

  public void fail(Throwable throwable) {
    if (exceptionHandler != null) {
      exceptionHandler.handle(throwable);
    }
  }

}
