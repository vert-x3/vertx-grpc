package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;

public class GrpcServerCallRequest<Req, Resp> {

  private final GrpcServerCallResponse<Req, Resp> response;
  private Handler<Req> messageHandler;
  private Handler<Void> endHandler;

  public GrpcServerCallRequest(GrpcServerRequest grpcRequest, MethodDescriptor<Req, Resp> methodDesc) {
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
