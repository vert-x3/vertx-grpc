package io.vertx.grpc.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.GzipOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.vertx.core.buffer.Buffer;

import java.util.Queue;

public interface GrpcMessage {

  static GrpcMessage message(Buffer payload) {
    return new Base(payload);
  }

  class Base implements GrpcMessage {

    private final Buffer data;

    public Base(Buffer data) {
      this.data = data;
    }

    @Override
    public boolean isCompressed() {
      return false;
    }

    @Override
    public Buffer payload() {
      return data;
    }

    @Override
    public Buffer encode(String encoding) {
      return encode(data, encoding);
    }

    private static Buffer encode(Buffer payload, String encoding) {
      ByteBuf byteBuf = payload.getByteBuf();
      CompositeByteBuf composite = Unpooled.compositeBuffer();
      boolean compressed;
      switch (encoding) {
        case "identity":
          composite.addComponent(true, byteBuf);
          compressed = false;
          break;
        case "gzip":
          compressed = true;
          GzipOptions options = StandardCompressionOptions.gzip();
          ZlibEncoder encoder = ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP, options.compressionLevel(), options.windowBits(), options.memLevel());
          EmbeddedChannel channel = new EmbeddedChannel(encoder);
          channel.writeOutbound(payload.getByteBuf());
          channel.finish();
          Queue<Object> messages = channel.outboundMessages();
          ByteBuf a;
          while ((a = (ByteBuf) messages.poll()) != null) {
            composite.addComponent(true, a);
          }
          channel.close();
          break;
        default:
          throw new UnsupportedOperationException();
      }
      int len = composite.readableBytes();
      ByteBufAllocator alloc = byteBuf.alloc();
      ByteBuf prefix = alloc.buffer(5, 5);
      prefix.writeByte(compressed ? 1 : 0);      // Compression flag
      prefix.writeInt(len);                      // Length
      composite.addComponent(true, 0, prefix);
      return Buffer.buffer(composite);
    }
  }

  class Compressed implements GrpcMessage {

    private String compression;
    private Buffer compressed;
    private Buffer payload;

    public Compressed(Buffer compressed, String compression) {
      this.compression = compression;
      this.compressed = compressed;
    }

    @Override
    public boolean isCompressed() {
      return true;
    }

    @Override
    public Buffer payload() {
      if (payload == null) {
        if ("gzip".equals(compression)) {
          EmbeddedChannel channel = new EmbeddedChannel(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
          try {
            channel.writeOneInbound(compressed.getByteBuf());
            channel.flushInbound();
            int o = channel.inboundMessages().size();
            if (o > 0) {
              payload = Buffer.buffer((ByteBuf) channel.inboundMessages().poll());
            }
          } finally {
            channel.close();
          }
        }
      }
      return payload;
    }

    @Override
    public Buffer encode(String compression) {
      throw new UnsupportedOperationException();
    }
  }

  boolean isCompressed();

  Buffer payload();

  default Buffer encode() {
    return encode("identity");
  }

  Buffer encode(String compression);

}
