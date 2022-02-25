package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;

import java.io.ByteArrayInputStream;

public class GrpcClientCallResponse<Req, Resp> {

  private final MethodDescriptor<Req, Resp> methodDesc;
  private Handler<Resp> messageHandler;
  private Handler<Void> endHandler;

  public GrpcClientCallResponse(GrpcClientResponse grpcResponse, MethodDescriptor<Req, Resp> methodDesc) {
    grpcResponse.messageHandler(msg -> {
      handleMessage(msg);
    });
    grpcResponse.endHandler(v -> {
      handleEnd();
    });
    this.methodDesc = methodDesc;
  }

  private void handleMessage(GrpcMessage message) {
    ByteArrayInputStream in = new ByteArrayInputStream(message.data().getBytes());
    Resp obj = methodDesc.parseResponse(in);
    Handler<Resp> handler = messageHandler;
    if (handler != null) {
      handler.handle(obj);
    }
  }

  private void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  public GrpcClientCallResponse<Req, Resp> messageHandler(Handler<Resp> handler) {
    messageHandler = handler;
    return this;
  }

  public GrpcClientCallResponse<Req, Resp> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }
}
