package io.vertx.ext.grpc;

import io.grpc.internal.AbstractReadableBuffer;
import io.grpc.internal.ReadableBuffer;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxReadableBuffer extends AbstractReadableBuffer {

  final Buffer buffer;
  private int index;

  public VertxReadableBuffer(Buffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public int readableBytes() {
    return buffer.length();
  }

  @Override
  public int readUnsignedByte() {
    return buffer.getUnsignedByte(index++);
  }

  @Override
  public void skipBytes(int length) {
    index += length;
  }

  @Override
  public void readBytes(byte[] dest, int destOffset, int length) {
    buffer.getBytes(index, index + length, dest, destOffset);
    index += length;
  }

  @Override
  public void readBytes(ByteBuffer dest) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readBytes(OutputStream dest, int length) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReadableBuffer readBytes(int length) {
    throw new UnsupportedOperationException();
  }
}
