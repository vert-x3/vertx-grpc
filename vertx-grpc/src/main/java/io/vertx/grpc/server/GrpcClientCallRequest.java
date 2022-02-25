package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.InputStream;

public class GrpcClientCallRequest<Req, Resp> {

  private final GrpcClientRequest grpcRequest;
  private final MethodDescriptor<Req, Resp> methodDesc;

  public GrpcClientCallRequest(GrpcClientRequest grpcRequest, MethodDescriptor<Req, Resp> methodDesc) {

    grpcRequest.fullMethodName(methodDesc.getFullMethodName());

    this.grpcRequest = grpcRequest;
    this.methodDesc = methodDesc;
  }

  public void write(Req message) {
    grpcRequest.write(new GrpcMessage(encode(message)));
  }

  public void end(Req message) {
    grpcRequest.end(new GrpcMessage(encode(message)));
  }

  public void end() {
    grpcRequest.end();
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
