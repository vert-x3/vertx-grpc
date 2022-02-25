package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;

public class GrpcServerCallRequest<Req, Resp> {

  final GrpcServerRequest grpcRequest;
  final MethodDescriptor<Req, Resp> methodDesc;
  final GrpcServerCallResponse<Req, Resp> response;
  Handler<Req> handler;
  Handler<Void> endHandler;

  public GrpcServerCallRequest(GrpcServerRequest grpcRequest, MethodDescriptor<Req, Resp> methodDesc) {
    this.grpcRequest = grpcRequest;
    this.methodDesc = methodDesc;
    this.response = new GrpcServerCallResponse<>(grpcRequest.response, methodDesc);
  }

  public GrpcServerCallRequest<Req, Resp> handler(Handler<Req> handler) {
    this.handler = handler;
    return this;
  }

  public GrpcServerCallRequest<Req, Resp> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public GrpcServerCallResponse<Req, Resp> response() {
    return response;
  }
}
