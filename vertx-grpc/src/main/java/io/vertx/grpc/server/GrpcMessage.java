package io.vertx.grpc.server;

import io.vertx.core.buffer.Buffer;

public class GrpcMessage {

  private final Buffer data;

  public GrpcMessage(Buffer data) {
    this.data = data;
  }

  public Buffer data() {
    return data;
  }
}
