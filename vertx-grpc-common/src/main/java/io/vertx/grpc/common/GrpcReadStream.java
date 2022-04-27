package io.vertx.grpc.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

@VertxGen
public interface GrpcReadStream<T> extends ReadStream<T> {

  /**
   * @return the {@link MultiMap} to read metadata headers
   */
  MultiMap headers();

  /**
   * @return the stream encoding, e.g {@code identity} or {@code gzip}
   */
  String encoding();

  /**
   * Set a handler to be notified with incoming encoded messages. The {@code handler} is
   * responsible for fully decoding incoming messages, including compression.
   *
   * @param handler the message handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcReadStream<T> messageHandler(@Nullable Handler<GrpcMessage> handler);

  @Override
  GrpcReadStream<T> exceptionHandler(@Nullable Handler<Throwable> handler);

  @Override
  GrpcReadStream<T> handler(@Nullable Handler<T> handler);

  @Override
  GrpcReadStream<T> pause();

  @Override
  GrpcReadStream<T> resume();

  @Override
  GrpcReadStream<T> fetch(long l);

  @Override
  GrpcReadStream<T> endHandler(@Nullable Handler<Void> handler);

  /**
   * @return the last element of the stream
   */
  Future<T> last();

  /**
   * @return the result of applying a collector on the stream
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  <R, A> Future<R> collecting(java.util.stream.Collector<T , A , R> collector);

  @GenIgnore
  default void pipeTo(WriteStream<T> dst, Handler<AsyncResult<Void>> handler) {
    ReadStream.super.pipeTo(dst, handler);
  }
}
