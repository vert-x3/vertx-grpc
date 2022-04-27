package io.vertx.grpc.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.streams.WriteStream;

@VertxGen
public interface GrpcWriteStream<T> extends WriteStream<T> {

  /**
   * @return the {@link MultiMap} to reader metadata headers
   */
  MultiMap headers();

  /**
   * Set the stream encoding, e.g {@code identity} or {@code gzip}.
   *
   * It must be called before sending any message, otherwise {@code identity will be used.
   *
   * @param encoding the target message encoding
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcWriteStream<T> encoding(String encoding);

  @Override
  GrpcWriteStream<T> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcWriteStream<T> setWriteQueueMaxSize(int i);

  @Override
  GrpcWriteStream<T> drainHandler(@Nullable Handler<Void> handler);

  /**
   * Write an encoded gRPC message.
   *
   * @param message the message
   * @return a future completed with the result
   */
  Future<Void> writeMessage(GrpcMessage message);

  /**
   * End the stream with an encoded gRPC message.
   *
   * @param message the message
   * @return a future completed with the result
   */
  Future<Void> endMessage(GrpcMessage message);

  /**
   * Cancel the stream.
   */
  void cancel();

  @GenIgnore
  @Override
  void write(T t, Handler<AsyncResult<Void>> handler);

  @GenIgnore
  @Override
  void end(Handler<AsyncResult<Void>> handler);

  @GenIgnore
  @Override
  default void end(T data, Handler<AsyncResult<Void>> handler) {
    WriteStream.super.end(data, handler);
  }
}
