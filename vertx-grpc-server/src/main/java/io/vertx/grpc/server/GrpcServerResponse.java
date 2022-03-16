package io.vertx.grpc.server;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;

@VertxGen
public interface GrpcServerResponse<Req, Resp> extends WriteStream<Resp> {

  /**
   * Set the grpc status response
   *
   * @param status the status
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcServerResponse<Req, Resp> status(GrpcStatus status);

  @Fluent
  GrpcServerResponse<Req, Resp> encoding(String encoding);

  @Override
  GrpcServerResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcServerResponse<Req, Resp> setWriteQueueMaxSize(int maxSize);

  @Override
  GrpcServerResponse<Req, Resp> drainHandler(@Nullable Handler<Void> handler);
}
