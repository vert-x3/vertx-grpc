/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.common;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
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
    public Buffer decode(GrpcMessage msg) throws CodecException {
      EmbeddedChannel channel = new EmbeddedChannel(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
      try {
        ChannelFuture fut = channel.writeOneInbound(msg.payload().getByteBuf());
        if (fut.isSuccess()) {
          Buffer decoded = null;
          while (true) {
            ByteBuf buf = channel.readInbound();
            if (buf == null) {
              break;
            }
            if (decoded == null) {
              decoded = Buffer.buffer(buf);
            } else {
              decoded.appendBuffer(Buffer.buffer(buf));
            }
          }
          if (decoded == null) {
            throw new CodecException("Invalid GZIP input");
          }
          return decoded;
        } else {
          throw new CodecException(fut.cause());
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

  T decode(GrpcMessage msg) throws CodecException;

}
