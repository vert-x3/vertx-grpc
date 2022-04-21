package io.vertx.grpc.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

@VertxGen
public interface GrpcWriteStream<T> extends WriteStream<T> {

  /**
   * @return the {@link MultiMap} to reader metadata headers
   */
  MultiMap headers();

  @Fluent
  GrpcWriteStream<T> encoding(String encoding);

  /**
   * Reset the stream.
   *
   * This is an HTTP/2 operation.
   */
  void reset();

  @Override
  GrpcWriteStream<T> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcWriteStream<T> setWriteQueueMaxSize(int i);

  @Override
  GrpcWriteStream<T> drainHandler(@Nullable Handler<Void> handler);

  Future<Void> write(T message);

  Future<Void> end(T message);

  Future<Void> writeMessage(GrpcMessage message);

  Future<Void> endMessage(GrpcMessage message);
}
