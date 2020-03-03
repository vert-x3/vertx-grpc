/*
 * Copyright 2019 Eclipse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.grpc.stub;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

/**
 *
 * @author ecatala
 */
public class GrpcWriteStream<T> implements WriteStream<T> {

  private final StreamObserver<T> observer;
  private Handler<Throwable> errHandler;

  public GrpcWriteStream(StreamObserver<T> observer) {
    this.observer = observer;
    this.errHandler = observer::onError;
  }

  @Override
  public GrpcWriteStream<T> exceptionHandler(Handler<Throwable> hndlr) {
    if (hndlr == null) {
      this.errHandler = observer::onError;
    } else {
      this.errHandler = (Throwable t) -> {
        observer.onError(t);
        hndlr.handle(t);
      };
    }
    return this;
  }

  @Override
  public GrpcWriteStream<T> write(T data) {
    observer.onNext(data);
    return this;
  }

  @Override
  public GrpcWriteStream<T> write(T data, Handler<AsyncResult<Void>> hndlr) {
    observer.onNext(data);
    hndlr.handle(Future.succeededFuture());
    return this;
  }

  @Override
  public void end() {
    observer.onCompleted();
  }

  @Override
  public void end(Handler<AsyncResult<Void>> hndlr) {
    observer.onCompleted();
    hndlr.handle(Future.succeededFuture());
  }

  @Override
  public WriteStream<T> setWriteQueueMaxSize(int i) {
    errHandler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public WriteStream<T> drainHandler(Handler<Void> hndlr) {
    errHandler.handle(new RuntimeException("Unsupported Operation"));
    return this;
  }

}
