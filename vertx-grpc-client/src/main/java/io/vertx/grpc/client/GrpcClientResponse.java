package io.vertx.grpc.client;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.streams.ReadStream;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;

/**
 * A response from a gRPC server.
 *
 * You can set a {@link #messageHandler(Handler)} to receive {@link GrpcMessage} and a {@link #endHandler(Handler)} to be notified
 * of the end of the response.
 *
 */
@VertxGen
public interface GrpcClientResponse<Req, Resp> extends ReadStream<Resp> {

  /**
   * @return the gRPC status or {@code null} when the status has not yet been received
   */
  @CacheReturn
  GrpcStatus status();

  /**
   * @return the {@link MultiMap} to write metadata headers
   */
  MultiMap headers();

  /**
   * @return the {@link MultiMap} to write metadata trailers
   */
  MultiMap trailers();

  /**
   * Set a handler to be notified with incoming messages.
   *
   * @param handler the message handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcClientResponse<Req, Resp> messageHandler(Handler<Resp> handler);

  @Override
  GrpcClientResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcClientResponse<Req, Resp> handler(Handler<Resp> handler);

  GrpcClientResponse<Req, Resp> endHandler(Handler<Void> handler);

  @Override
  GrpcClientResponse<Req, Resp> pause();

  @Override
  GrpcClientResponse<Req, Resp> resume();

  @Override
  GrpcClientResponse<Req, Resp> fetch(long amount);
}
