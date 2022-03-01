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

public class GrpcServerCallResponse<Req, Resp> implements WriteStream<Resp> {

  final GrpcServerResponse grpcResponse;
  final MethodDescriptor<Req, Resp> methodDesc;

  public GrpcServerCallResponse(GrpcServerResponse grpcResponse, MethodDescriptor<Req, Resp> methodDesc) {
    this.grpcResponse = grpcResponse;
    this.methodDesc = methodDesc;
  }

  public GrpcServerCallResponse<Req, Resp> encoding(String encoding) {
    grpcResponse.encoding(encoding);
    return this;
  }

  public Future<Void> write(Resp message) {
    return grpcResponse.write(GrpcMessage.message(encode(message)));
  }

  @Override
  public GrpcServerCallResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    grpcResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcServerCallResponse<Req, Resp> drainHandler(Handler<Void> handler) {
    grpcResponse.drainHandler(handler);
    return this;
  }

  @Override
  public void write(Resp data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  public Future<Void> end() {
    return grpcResponse.end();
  }

  @Override
  public GrpcServerCallResponse<Req, Resp> setWriteQueueMaxSize(int maxSize) {
    grpcResponse.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return grpcResponse.writeQueueFull();
  }

  private Buffer encode(Resp resp) {
    Buffer encoded = Buffer.buffer();
    InputStream stream = methodDesc.streamResponse(resp);
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
