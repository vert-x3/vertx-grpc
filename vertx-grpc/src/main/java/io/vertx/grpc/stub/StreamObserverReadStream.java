/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.grpc.stub;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

/**
 * @author Rogelio Orts
 */
class StreamObserverReadStream<T> implements StreamObserver<T>, ReadStream<T> {

  private Handler<Throwable> exceptionHandler;

  private Handler<T> handler;

  private Handler<Void> endHandler;

  @Override
  public void onNext(T t) {
    if (handler != null) {
      handler.handle(t);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    if (exceptionHandler != null) {
      exceptionHandler.handle(throwable);
    }
  }

  @Override
  public void onCompleted() {
    if (endHandler != null) {
      endHandler.handle(null);
    }
  }

  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;

    return this;
  }

  @Override
  public ReadStream<T> handler(@Nullable Handler<T> handler) {
    this.handler = handler;

    return this;
  }

  @Override
  public ReadStream<T> pause() {
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    return this;
  }

  @Override
  public ReadStream<T> fetch(long l) {
    return this;
  }

  @Override
  public ReadStream<T> endHandler(@Nullable Handler<Void> handler) {
    this.endHandler = handler;

    return this;
  }

}
