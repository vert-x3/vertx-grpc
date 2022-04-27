package io.vertx.grpc.common;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

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
  GrpcReadStream<T> messageHandler(Handler<GrpcMessage> handler);

  @Override
  GrpcReadStream<T> exceptionHandler(Handler<Throwable> handler);

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

}
