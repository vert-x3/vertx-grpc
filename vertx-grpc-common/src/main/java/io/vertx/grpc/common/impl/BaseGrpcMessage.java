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
package io.vertx.grpc.common.impl;

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
import io.vertx.grpc.common.GrpcMessage;

import java.util.Queue;

public class BaseGrpcMessage implements GrpcMessage {

  private final Buffer data;

  public BaseGrpcMessage(Buffer data) {
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
