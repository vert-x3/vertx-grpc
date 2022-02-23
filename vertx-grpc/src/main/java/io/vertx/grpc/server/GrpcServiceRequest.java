package io.vertx.grpc.server;

import io.grpc.ServerMethodDefinition;
import io.vertx.core.Handler;

public class GrpcServiceRequest {

  private ServerMethodDefinition<?, ?> methodDefinition;
  private GrpcServiceResponse response;
  Handler<GrpcMessage> messageHandler;
  Handler<Void> endHandler;

  public GrpcServiceRequest(GrpcServiceResponse response, ServerMethodDefinition<?, ?> methodDefinition) {
    this.response = response;
    this.methodDefinition = methodDefinition;
  }

  public ServerMethodDefinition<?, ?> methodDefinition() {
    return methodDefinition;
  }

  public GrpcServiceRequest messageHandler(Handler<GrpcMessage> messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public GrpcServiceRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public GrpcServiceResponse response() {
    return response;
  }
}
