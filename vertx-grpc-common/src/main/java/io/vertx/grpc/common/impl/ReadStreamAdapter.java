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

import io.vertx.core.streams.ReadStream;

import java.util.LinkedList;

/**
 * An adapter between gRPC and Vert.x back-pressure.
 */
public class ReadStreamAdapter<T> {

  private static final int MAX_INFLIGHT_MESSAGES = 16;

  private final LinkedList<T> queue = new LinkedList<>();
  private int requests = 0;
  private boolean ended;
  private boolean closed;
  private ReadStream<T> stream;

  /**
   * Init the adapter with the stream.
   */
  public final void init(ReadStream<T> stream) {
    stream.handler(msg -> {
      synchronized (queue) {
        queue.add(msg);
        if (queue.size() > MAX_INFLIGHT_MESSAGES) {
          stream.pause();
        }
      }
      checkPending();
    });
    stream.endHandler(v -> {
      synchronized (queue) {
        ended = true;
      }
      checkPending();
    });
    this.stream = stream;
  }

  /**
   * Override this to handle close event
   */
  protected void handleClose() {

  }

  /**
   * Override this to handle message event
   */
  protected void handleMessage(T msg) {

  }

  /**
   * Request {@code num} messages
   */
  public final void request(int num) {
    synchronized (queue) {
      requests += num;
    }
    checkPending();
  }

  private void checkPending() {
    boolean doResume = false;
    while (true) {
      T msg;
      synchronized (queue) {
        if (queue.isEmpty()) {
          if (!ended || closed) {
            break;
          }
          closed = true;
          msg = null;
        } else {
          if (requests == 0) {
            break;
          }
          if (queue.size() == MAX_INFLIGHT_MESSAGES) {
            doResume = true;
          }
          requests--;
          msg = queue.poll();
        }
      }
      if (msg == null) {
        handleClose();
      } else {
        handleMessage(msg);
      }
    }
    if (doResume) {
      stream.resume();
    }
  }
}
