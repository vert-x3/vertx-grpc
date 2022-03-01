package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.io.ByteArrayInputStream;

public class GrpcClientCallResponse<Req, Resp> implements ReadStream<Resp> {

  private final GrpcClientResponse grpcResponse;
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
    this.grpcResponse = grpcResponse;
    this.methodDesc = methodDesc;
  }

  private void handleMessage(GrpcMessage message) {
    ByteArrayInputStream in = new ByteArrayInputStream(message.payload().getBytes());
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

  @Override
  public ReadStream<Resp> exceptionHandler(Handler<Throwable> handler) {
    grpcResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcClientCallResponse<Req, Resp> handler(@Nullable Handler<Resp> handler) {
    return messageHandler(handler);
  }

  @Override
  public GrpcClientCallResponse<Req, Resp> pause() {
    grpcResponse.pause();
    return this;
  }

  @Override
  public GrpcClientCallResponse<Req, Resp> resume() {
    grpcResponse.resume();
    return this;
  }

  @Override
  public GrpcClientCallResponse<Req, Resp> fetch(long amount) {
    grpcResponse.fetch(amount);
    return this;
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
