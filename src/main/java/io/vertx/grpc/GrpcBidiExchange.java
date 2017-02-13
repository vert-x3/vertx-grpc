package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.grpc.impl.GrpcBidiExchangeImpl;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface GrpcBidiExchange<I, O> extends GrpcReadStream<I>, GrpcWriteStream<O> {

  static <I, O> GrpcBidiExchange<I, O> create(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    return new GrpcBidiExchangeImpl<>(readStream, writeObserver);
  }

  static <I, O> GrpcBidiExchange<I, O> create(StreamObserver<I> readObserver, StreamObserver<O> writeObserver) {
    return new GrpcBidiExchangeImpl<>(readObserver, writeObserver);
  }

  @Fluent
  @Override
  GrpcBidiExchange<I, O> exceptionHandler(Handler<Throwable> handler);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> write(O data);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> setWriteQueueMaxSize(int maxSize);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> drainHandler(Handler<Void> handler);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> fail(Throwable t);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> handler(Handler<I> handler);

  @Fluent
  @Override
  GrpcBidiExchange<I, O> pause();

  @Fluent
  @Override
  GrpcBidiExchange<I, O> resume();

  @Fluent
  @Override
  GrpcBidiExchange<I, O> endHandler(Handler<Void> handler);

  @GenIgnore
  @Override
  StreamObserver<I> readObserver();

  @GenIgnore
  @Override
  GrpcBidiExchange<I, O> setReadObserver(StreamObserver<I> observer);

  @GenIgnore
  @Override
  StreamObserver<O> writeObserver();
}
