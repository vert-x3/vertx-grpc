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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcReadStream;

import java.util.function.BiConsumer;
import java.util.stream.Collector;

/**
 * Transforms {@code Buffer} into a stream of {@link GrpcMessage}
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class GrpcReadStreamBase<S extends GrpcReadStreamBase<S, T>, T> implements GrpcReadStream<T>, Handler<Buffer> {

  static final GrpcMessage END_SENTINEL = new GrpcMessage() {
    @Override
    public String encoding() {
      return null;
    }
    @Override
    public Buffer payload() {
      return null;
    }
  };

  protected final ContextInternal context;
  private final String encoding;
  private final ReadStream<Buffer> stream;
  private final InboundBuffer<GrpcMessage> queue;
  private Buffer buffer;

  protected GrpcReadStreamBase(Context context, ReadStream<Buffer> stream, String encoding) {
    this.context = (ContextInternal) context;
    this.encoding = encoding;
    this.stream = stream;
    this.queue = new InboundBuffer<>(context);
  }

  public void init() {
    stream.handler(this);
    stream.endHandler(v -> queue.write(END_SENTINEL));
    stream.exceptionHandler(err -> {
      if (err instanceof StreamResetException) {
        handleReset(((StreamResetException)err).getCode());
      } else {
        handleException(err);
      }
    });
    queue.drainHandler(v -> stream.resume());
    queue.handler(msg -> {
      if (msg == END_SENTINEL) {
        handleEnd();
      } else {
        handleMessage(msg);
      }
    });
  }

  public void handle(Buffer chunk) {
    if (buffer == null) {
      buffer = chunk;
    } else {
      buffer.appendBuffer(chunk);
    }
    int idx = 0;
    boolean pause = false;
    int len;
    while (idx + 5 <= buffer.length() && (idx + 5 + (len = buffer.getInt(idx + 1)))<= buffer.length()) {
      boolean compressed = buffer.getByte(idx) == 1;
      if (compressed && encoding == null) {
        throw new UnsupportedOperationException("Handle me");
      }
      Buffer payload = buffer.slice(idx + 5, idx + 5 + len);
      GrpcMessage message = GrpcMessage.message(compressed ? encoding : "identity", payload);
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

  public S pause() {
    queue.pause();
    return (S) this;
  }

  public S resume() {
    queue.resume();
    return (S) this;
  }

  public S fetch(long amount) {
    queue.fetch(amount);
    return (S) this;
  }

  protected void handleReset(long code) {
  }

  protected void handleException(Throwable err) {
  }

  protected void handleEnd() {
  }

  protected void handleMessage(GrpcMessage msg) {
  }

  @Override
  public Future<T> last() {
    PromiseInternal<T> promise = context.promise();
    Object[] last = new Object[1];
    handler(elt -> last[0] = elt);
    endHandler(v -> promise.tryComplete((T) last[0]));
    exceptionHandler(err -> promise.tryFail(err));
    return promise.future();
  }

  @Override
  public <R, C> Future<R> collecting(Collector<T, C, R> collector) {
    PromiseInternal<R> promise = context.promise();
    C cumulation = collector.supplier().get();
    BiConsumer<C, T> accumulator = collector.accumulator();
    handler(elt -> accumulator.accept(cumulation, elt));
    endHandler(v -> {
      R result = collector.finisher().apply(cumulation);
      promise.tryComplete(result);
    });
    exceptionHandler(promise::tryFail);
    return promise.future();
  }
}
