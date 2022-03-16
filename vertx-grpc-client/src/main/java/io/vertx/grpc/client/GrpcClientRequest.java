package io.vertx.grpc.client;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.ServiceName;

/**
 * A request to a gRPC server.
 *
 * <p>You interact with the remote service with Protobuf encoded messages.
 *
 * <p>Before sending a request you need to set {@link #serviceName)} and {@link #methodName)} or
 * alternatively the service {@link #fullMethodName}.
 *
 * <p>Writing a request message will send the request to the service:
 *
 * <ul>
 *   <li>To send a unary request, just call {@link #end(Req)}</li>
 *   <li>To send a streaming request, call {@link #write(Req)} any time you need and then {@link #end()}</li>
 * </ul>
 */
public interface GrpcClientRequest<Req, Resp> extends WriteStream<Req> {

  @Fluent
  GrpcClientRequest<Req, Resp> encoding(String encoding);

  /**
   * Set the full method name to call, it must follow the format {@code package-name + '.' + service-name + '/' + method-name}
   * or an {@code IllegalArgumentException} is thrown.
   *
   * <p> It must be called before sending the request otherwise an {@code IllegalStateException} is thrown.
   *
   * @param fullMethodName the full method name to call
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcClientRequest<Req, Resp> fullMethodName(String fullMethodName);

  /**
   * Set the service name to call.
   *
   * <p> It must be called before sending the request otherwise an {@code IllegalStateException} is thrown.
   *
   * @param serviceName the service name to call
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcClientRequest<Req, Resp> serviceName(ServiceName serviceName);

  /**
   * Set the method name to call.
   *
   * <p> It must be called before sending the request otherwise an {@code IllegalStateException} is thrown.
   *
   * @param methodName the method name to call
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcClientRequest<Req, Resp> methodName(String methodName);

  /**
   * @return the grpc response
   */
  @CacheReturn
  Future<GrpcClientResponse<Req, Resp>> response();

  @Override
  GrpcClientRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler);

  @Override
  void write(Req data, Handler<AsyncResult<Void>> handler);

  @Override
  void end(Handler<AsyncResult<Void>> handler);

  @Override
  GrpcClientRequest<Req, Resp> setWriteQueueMaxSize(int maxSize);

  @Override
  boolean writeQueueFull();

  @Override
  GrpcClientRequest<Req, Resp> drainHandler(Handler<Void> handler);

  Future<Void> write(Req message);

  Future<Void> end(Req message);

  Future<Void> end();

}
