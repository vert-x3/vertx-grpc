package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

public class GrpcServerCallRequest<Req, Resp> implements ReadStream<Req> {

  private final GrpcServerRequest grpcRequest;
  private final GrpcServerCallResponse<Req, Resp> response;
  private Handler<Req> messageHandler;
  private Handler<Void> endHandler;

  public GrpcServerCallRequest(GrpcServerRequest grpcRequest, MethodDescriptor<Req, Resp> methodDesc) {
    this.grpcRequest = grpcRequest;
    this.response = new GrpcServerCallResponse<>(grpcRequest.response(), methodDesc);
  }

  public GrpcServerCallRequest<Req, Resp> handler(Handler<Req> handler) {
    this.messageHandler = handler;
    return this;
  }

  public GrpcServerCallRequest<Req, Resp> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public GrpcServerCallResponse<Req, Resp> response() {
    return response;
  }

  @Override
  public GrpcServerCallRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    grpcRequest.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcServerCallRequest<Req, Resp> pause() {
    grpcRequest.pause();
    return this;
  }

  @Override
  public GrpcServerCallRequest<Req, Resp> resume() {
    grpcRequest.resume();
    return this;
  }

  @Override
  public GrpcServerCallRequest<Req, Resp> fetch(long amount) {
    grpcRequest.fetch(amount);
    return this;
  }

  void handleMessage(Req message) {
    Handler<Req> handler = messageHandler;
    if (handler != null) {
      handler.handle(message);
    }
  }

  void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }
}
