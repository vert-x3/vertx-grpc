package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.grpc.impl.GrpcUniExchangeImpl;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public interface GrpcUniExchange<O,I> extends GrpcWriteStream<O> {

  static <O,I> GrpcUniExchange<O,I> create(GrpcReadStream<I> read, StreamObserver<O> write) {
    return new GrpcUniExchangeImpl<>(read, write);
  }

  @Fluent
  GrpcUniExchange<O,I> handler(Handler<AsyncResult<I>> handler);

  @Override
  @Fluent
  GrpcUniExchange<O,I> write(O data);

  @Override
  @Fluent
  GrpcUniExchange<O,I> setWriteQueueMaxSize(int maxSize);

  @Override
  @Fluent
  GrpcUniExchange<O,I> drainHandler(@Nullable Handler<Void> handler);

  @Override
  @Fluent
  GrpcUniExchange<O,I> fail(Throwable t);

  @Override
  @GenIgnore
  StreamObserver<O> writeObserver();
}
