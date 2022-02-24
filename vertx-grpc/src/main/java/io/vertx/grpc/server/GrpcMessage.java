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

  public Buffer encode() {
    Buffer message = Buffer.buffer(data.length());
    message.appendByte((byte)0);      // Compression
    message.appendInt(data.length()); // Length
    message.appendBuffer(data);
    return message;
  }
}
