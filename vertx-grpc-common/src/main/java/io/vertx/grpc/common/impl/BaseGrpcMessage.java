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
  private final String encoding;

  public BaseGrpcMessage(Buffer data, String encoding) {
    this.data = data;
    this.encoding = encoding;
  }

  @Override
  public String encoding() {
    return encoding;
  }

  @Override
  public Buffer payload() {
    return data;
  }

  public static Buffer encode(GrpcMessage message) {
    ByteBuf bbuf = message.payload().getByteBuf();
    int len = bbuf.readableBytes();
    boolean compressed = !message.encoding().equals("identity");
    ByteBuf prefix = Unpooled.buffer(5, 5);
    prefix.writeByte(compressed ? 1 : 0);      // Compression flag
    prefix.writeInt(len);                      // Length
    CompositeByteBuf composite = Unpooled.compositeBuffer();
    composite.addComponent(true, prefix);
    composite.addComponent(true, bbuf);
    return Buffer.buffer(composite);
  }
}
