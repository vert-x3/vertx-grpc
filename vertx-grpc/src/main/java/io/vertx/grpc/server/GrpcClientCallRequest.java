package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.InputStream;

public class GrpcClientCallRequest<Req, Resp> implements WriteStream<Req> {

  private final GrpcClientRequest grpcRequest;
  private final MethodDescriptor<Req, Resp> methodDesc;

  public GrpcClientCallRequest(GrpcClientRequest grpcRequest, MethodDescriptor<Req, Resp> methodDesc) {

    grpcRequest.fullMethodName(methodDesc.getFullMethodName());

    this.grpcRequest = grpcRequest;
    this.methodDesc = methodDesc;
  }

  public GrpcClientCallRequest<Req, Resp> encoding(String encoding) {
    grpcRequest.encoding(encoding);
    return this;
  }

  @Override
  public GrpcClientCallRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    grpcRequest.exceptionHandler(handler);
    return this;
  }

  @Override
  public void write(Req data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override
  public GrpcClientCallRequest<Req, Resp> setWriteQueueMaxSize(int maxSize) {
    grpcRequest.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return grpcRequest.writeQueueFull();
  }

  @Override
  public GrpcClientCallRequest<Req, Resp> drainHandler(Handler<Void> handler) {
    grpcRequest.drainHandler(handler);
    return this;
  }

  public Future<Void> write(Req message) {
    return grpcRequest.write(GrpcMessage.message(encode(message)));
  }

  public Future<Void> end(Req message) {
    return grpcRequest.end(GrpcMessage.message(encode(message)));
  }

  public Future<Void> end() {
    return grpcRequest.end();
  }

  public Future<GrpcClientCallResponse<Req, Resp>> response() {
    return grpcRequest.response().map(grpcResponse -> new GrpcClientCallResponse<>(grpcResponse, methodDesc));
  }

  private Buffer encode(Req message) {
    Buffer encoded = Buffer.buffer();
    InputStream stream = methodDesc.streamRequest(message);
    byte[] tmp = new byte[256];
    int i;
    try {
      while ((i = stream.read(tmp)) != -1) {
        encoded.appendBytes(tmp, 0, i);
      }
    } catch (IOException e) {
      throw new VertxException(e);
    }
    return encoded;
  }
}
