package io.vertx.grpc.server;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

abstract class GrpcMessageDecoder implements Handler<Buffer> {

  static final GrpcMessage END_SENTINEL = new GrpcMessage(Buffer.buffer());

  private final ReadStream<Buffer> stream;
  private final InboundBuffer<GrpcMessage> queue;
  private Buffer buffer;

  GrpcMessageDecoder(Context context, ReadStream<Buffer> stream) {
    this.stream = stream;
    this.queue = new InboundBuffer<>(context);
  }

  void init() {
    stream.handler(this);
    stream.endHandler(v -> queue.write(END_SENTINEL));
    queue.drainHandler(v -> stream.resume());
    queue.handler(msg -> {
      if (msg == END_SENTINEL) {
        handleEnd();
      } else {
        handleMessage(msg);
      }
    });
  }

  protected void handleEnd() {

  }

  protected void handleMessage(GrpcMessage msg) {

  }

  @Override
  public void handle(Buffer chunk) {
    if (buffer == null) {
      buffer = chunk;
    } else {
      buffer.appendBuffer(chunk);
    }
    int idx = 0;
    boolean pause = false;
    int len;
    while (idx + 5 <= buffer.length() && (len = buffer.getInt(idx + 1)) + 5 <= buffer.length()) {
      Buffer data = buffer.slice(idx + 5, idx + 5 + len);
      GrpcMessage message = new GrpcMessage(data);
      pause |= !queue.write(message);
      idx += 5 + len;
    }
    if (pause) {
      stream.pause();
    }
    if (idx < buffer.length()) {
      buffer = buffer.getBuffer(idx, buffer.length());
    } else {
      buffer = null;
    }
  }

  public GrpcMessageDecoder pause() {
    queue.pause();
    return this;
  }

  public GrpcMessageDecoder resume() {
    queue.resume();
    return this;
  }

  public GrpcMessageDecoder fetch(long amount) {
    queue.fetch(amount);
    return this;
  }
}
