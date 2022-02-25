package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;

import java.io.ByteArrayInputStream;

public class GrpcClientCallResponse<Req, Resp> {

  private final GrpcClientResponse grpcResponse;
  private final MethodDescriptor<Req, Resp> methodDesc;
  private Handler<Resp> messageHandler;
  private Handler<Void> endHandler;

  public GrpcClientCallResponse(GrpcClientResponse grpcResponse, MethodDescriptor<Req, Resp> methodDesc) {

    grpcResponse.handler(msg -> {
      ByteArrayInputStream in = new ByteArrayInputStream(msg.data().getBytes());
      Resp obj = methodDesc.parseResponse(in);
      Handler<Resp> handler = messageHandler;
      if (handler != null) {
        handler.handle(obj);
      }

    });
    grpcResponse.endHandler(v -> {
      Handler<Void> handler = endHandler;
      if (handler != null) {
        handler.handle(null);
      }
    });

    this.methodDesc = methodDesc;
    this.grpcResponse = grpcResponse;
  }

  public GrpcClientCallResponse<Req, Resp> handler(Handler<Resp> handler) {
    messageHandler = handler;
    return this;
  }

  public GrpcClientCallResponse<Req, Resp> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }
}
