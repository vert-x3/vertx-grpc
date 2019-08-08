package io.vertx.ext.grpc.utils;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.function.Function;
import java.util.function.Supplier;

public class IterableReadStream<T> extends BaseReadStream<T> {

  private final Function<Integer, T> builder;

  private final int numIterations;

  public IterableReadStream(Function<Integer, T> builder, int numIterations) {
    this.builder = builder;
    this.numIterations = numIterations;
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    for(int i = 0; i < numIterations; i++) {
      handler.handle(builder.apply(i));
    }
    if (endHandler != null) {
      endHandler.handle(null);
    }

    return this;
  }

}
