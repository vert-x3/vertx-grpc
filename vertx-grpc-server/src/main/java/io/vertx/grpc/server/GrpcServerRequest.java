package io.vertx.grpc.server;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.ServiceName;

@VertxGen
public interface GrpcServerRequest<Req, Resp> extends ReadStream<Req> {

  /**
   * @return the service name
   */
  @CacheReturn
  ServiceName serviceName();

  /**
   * @return the method name
   */
  @CacheReturn
  String methodName();

  /**
   * @return the full method name sent by the client
   */
  @CacheReturn
  String fullMethodName();

  /**
   * Set a handler to be notified with incoming messages.
   *
   * @param handler the message handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcServerRequest<Req, Resp> messageHandler(Handler<Req> handler);

  /**
   * @return the response
   */
  @CacheReturn
  GrpcServerResponse<Req, Resp> response();

  @Override
  GrpcServerRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcServerRequest<Req, Resp> handler(@Nullable Handler<Req> handler);

  @Override
  GrpcServerRequest<Req, Resp> pause();

  @Override
  GrpcServerRequest<Req, Resp> resume();

  @Override
  GrpcServerRequest<Req, Resp> fetch(long amount);

  @Override
  GrpcServerRequest<Req, Resp> endHandler(@Nullable Handler<Void> endHandler);
}
