package io.vertx.grpc.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.vertx.core.buffer.Buffer;

public class GrpcMessage {

  private final Buffer data;

  public GrpcMessage(Buffer data) {
    this.data = data;
  }

  public Buffer data() {
    return data;
  }

  Buffer encoded() {
    ByteBuf payload = data.getByteBuf();
    ByteBufAllocator alloc = payload.alloc();
    ByteBuf prefix = alloc.buffer(5, 5);
    prefix.writeByte(0);            // Compression
    prefix.writeInt(data.length()); // Length
    CompositeByteBuf composite = alloc.compositeBuffer(2);
    composite.addComponent(prefix);
    composite.addComponent(payload);
    composite.readerIndex(0);
    composite.writerIndex(5 + data.length());
    return Buffer.buffer(composite);
  }
}
