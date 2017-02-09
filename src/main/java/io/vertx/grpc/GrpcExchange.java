package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.grpc.impl.GrpcExchangeImpl;

@VertxGen
public interface GrpcExchange<I, O> extends GrpcReadStream<I>, GrpcWriteStream<O> {

  static <I, O> GrpcExchange<I, O> create(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    return new GrpcExchangeImpl<>(readStream, writeObserver);
  }

  static <I, O> GrpcExchange<I, O> create(StreamObserver<I> readObserver, StreamObserver<O> writeObserver) {
    return new GrpcExchangeImpl<>(readObserver, writeObserver);
  }

  @Fluent
  @Override
  GrpcExchange<I, O> exceptionHandler(Handler<Throwable> handler);

  @Fluent
  @Override
  GrpcExchange<I, O> write(O data);

  @Fluent
  @Override
  GrpcExchange<I, O> setWriteQueueMaxSize(int maxSize);

  @Fluent
  @Override
  GrpcExchange<I, O> drainHandler(Handler<Void> handler);

  @Fluent
  @Override
  GrpcExchange<I, O> fail(Throwable t);

  @Fluent
  @Override
  GrpcExchange<I, O> handler(Handler<I> handler);

  @Fluent
  @Override
  GrpcExchange<I, O> pause();

  @Fluent
  @Override
  GrpcExchange<I, O> resume();

  @Fluent
  @Override
  GrpcExchange<I, O> endHandler(Handler<Void> handler);

  @GenIgnore
  @Override
  StreamObserver<I> readObserver();

  @GenIgnore
  @Override
  StreamObserver<O> writeObserver();
}
