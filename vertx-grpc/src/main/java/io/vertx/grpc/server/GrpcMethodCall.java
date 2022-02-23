package io.vertx.grpc.server;

import io.grpc.ServerMethodDefinition;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.InputStream;

public class GrpcMethodCall<Req, Resp> {

  final GrpcRequest request;
  final ServerMethodDefinition<Req, Resp> def;
  Handler<Req> handler;
  Handler<Void> endHandler;

  public GrpcMethodCall(GrpcRequest request, ServerMethodDefinition<Req, Resp> def) {
    this.request = request;
    this.def = def;
  }

  public GrpcMethodCall<Req, Resp> handler(Handler<Req> handler) {
    this.handler = handler;
    return this;
  }

  public GrpcMethodCall<Req, Resp> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public void write(Resp message) {
    request.write(encode(message));
  }

  public void end(Resp message) {
    request.end(encode(message));
  }

  private Buffer encode(Resp resp) {
    Buffer encoded = Buffer.buffer();
    InputStream stream = def.getMethodDescriptor().streamResponse(resp);
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
    request.end();
  }
}
