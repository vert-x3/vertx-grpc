package io.vertx.grpc.server;

import io.grpc.ServerMethodDefinition;
import io.vertx.core.Handler;

public class GrpcRequest {

  private ServerMethodDefinition<?, ?> methodDefinition;
  private GrpcResponse response;
  Handler<GrpcMessage> messageHandler;
  Handler<Void> endHandler;

  public GrpcRequest(GrpcResponse response, ServerMethodDefinition<?, ?> methodDefinition) {
    this.response = response;
    this.methodDefinition = methodDefinition;
  }

  public ServerMethodDefinition<?, ?> methodDefinition() {
    return methodDefinition;
  }

  public GrpcRequest messageHandler(Handler<GrpcMessage> messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public GrpcRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public GrpcResponse response() {
    return response;
  }
}
