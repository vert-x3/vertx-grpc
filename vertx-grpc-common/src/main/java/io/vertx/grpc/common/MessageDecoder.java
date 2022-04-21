package io.vertx.grpc.common;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayInputStream;

@VertxGen
public interface MessageDecoder<T> {

  MessageDecoder<Buffer> IDENTITY = new MessageDecoder<Buffer>() {
    @Override
    public Buffer decode(GrpcMessage msg) {
      return msg.payload();
    }
  };


  MessageDecoder<Buffer> GZIP = new MessageDecoder<Buffer>() {
    @Override
    public Buffer decode(GrpcMessage msg) {
      EmbeddedChannel channel = new EmbeddedChannel(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
      try {
        channel.writeOneInbound(msg.payload().getByteBuf());
        channel.flushInbound();
        int o = channel.inboundMessages().size();
        if (o > 0) {
          Buffer decoded = Buffer.buffer((ByteBuf) channel.inboundMessages().poll());
          return decoded;
        } else {
          throw new UnsupportedOperationException();
        }
      } finally {
        channel.close();
      }
    }
  };

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static <T> MessageDecoder<T> unmarshaller(MethodDescriptor.Marshaller<T> desc) {
    return new MessageDecoder<T>() {
      @Override
      public T decode(GrpcMessage msg) {
        ByteArrayInputStream in = new ByteArrayInputStream(msg.payload().getBytes());
        T data = desc.parse(in);
        return data;
      }
    };
  }

  T decode(GrpcMessage msg);

}
