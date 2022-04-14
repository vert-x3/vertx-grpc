package io.vertx.grpc.common.impl;

import io.vertx.core.streams.WriteStream;

/**
 * An adapter between gRPC and Vert.x back-pressure.
 */
public class WriteStreamAdapter<T> {

  private WriteStream<T> stream;
  private boolean ready;

  /**
   * Override this method to call gRPC {@code onReady}
   */
  protected void handleReady() {
  }

  public final void init(WriteStream<T> stream) {
    synchronized (this) {
      this.stream = stream;
    }
    stream.drainHandler(v -> {
      checkReady();
    });
    checkReady();
  }

  public final synchronized boolean isReady() {
    return ready;
  }

  public final void write(T msg) {
    stream.write(msg);
    synchronized (this) {
      ready = !stream.writeQueueFull();
    }
  }

  private void checkReady() {
    synchronized (this) {
      if (ready || stream.writeQueueFull()) {
        return;
      }
      ready = true;
    }
    handleReady();
  }
}
