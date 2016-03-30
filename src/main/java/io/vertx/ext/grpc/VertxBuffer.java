package io.vertx.ext.grpc;

import io.grpc.internal.WritableBuffer;
import io.vertx.core.buffer.Buffer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxBuffer implements WritableBuffer {

  final Buffer buffer = Buffer.buffer();
  private int writableBytes;
  private int readableBytes;

  public VertxBuffer(int capacity) {
    writableBytes = capacity;
  }

  @Override
  public void write(byte[] src, int srcIndex, int length) {
    buffer.appendBytes(src, srcIndex, length);
    writableBytes -= length;
    readableBytes += length;
  }

  @Override
  public int writableBytes() {
    return writableBytes;
  }

  @Override
  public int readableBytes() {
    return readableBytes;
  }

  @Override
  public void release() {
  }
}
