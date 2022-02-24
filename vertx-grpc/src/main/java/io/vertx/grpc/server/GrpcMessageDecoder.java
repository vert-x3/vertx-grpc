package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

public class GrpcMessageDecoder implements Handler<Buffer> {

  @Override
  public void handle(Buffer envelope) {
    int idx = 0;
    while (idx < envelope.length()) {
      int len = envelope.getInt(idx + 1);
      Buffer data = envelope.slice(idx + 5, idx + 5 + len);
      GrpcMessage msg = new GrpcMessage(data);
      handle(msg);
      idx += 5 + len;
    }
  }

  public void handle(GrpcMessage msg) {
  }
}
