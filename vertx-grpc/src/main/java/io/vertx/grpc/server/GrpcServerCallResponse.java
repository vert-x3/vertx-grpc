package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.InputStream;

public class GrpcServerCallResponse<Req, Resp> {

  final GrpcServerResponse grpcResponse;
  final MethodDescriptor<Req, Resp> methodDesc;

  public GrpcServerCallResponse(GrpcServerResponse grpcResponse, MethodDescriptor<Req, Resp> methodDesc) {
    this.grpcResponse = grpcResponse;
    this.methodDesc = methodDesc;
  }

  public void write(Resp message) {
    grpcResponse.write(encode(message));
  }

  public void end(Resp message) {
    grpcResponse.end(encode(message));
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

  public void end() {
    grpcResponse.end();
  }
}
