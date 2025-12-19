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

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Rogelio Orts
 * @author Eduard Català
 * @deprecated instead use Vert.x gRPC
 */
@Deprecated(forRemoval = true)
public final class ServerCalls {

  private ServerCalls() {
  }

  public static <I, O> void oneToOne(I request, StreamObserver<O> response, String compression, Function<I, Future<O>> delegate) {
    trySetCompression(response, compression);
    try {
      Future<O> future = delegate.apply(request);

      future.onComplete(res -> {
        if (res.succeeded()) {
          response.onNext(res.result());
          response.onCompleted();
        } else {
          response.onError(prepareError(res.cause()));
        }
      });

    } catch (Throwable throwable) {
      response.onError(prepareError(throwable));
    }
  }

  public static <I, O> void oneToMany(I request, StreamObserver<O> response, String compression, BiConsumer<I, WriteStream<O>> delegate) {
    trySetCompression(response, compression);
    try {
      GrpcWriteStream<O> responseWriteStream = new GrpcWriteStream<>(response);
      delegate.accept(request, responseWriteStream);
    } catch (Throwable throwable) {
      response.onError(prepareError(throwable));
    }
  }

  public static <I, O> StreamObserver<I> manyToOne(StreamObserver<O> response, String compression, Function<ReadStream<I>, Future<O>> delegate) {

    trySetCompression(response, compression);
    StreamObserverReadStream<I> request = new StreamObserverReadStream<>();
    Future<O> future = delegate.apply(request);
    future.onComplete(res -> {
      if (res.succeeded()) {
        response.onNext(res.result());
        response.onCompleted();
      } else {
        response.onError(prepareError(res.cause()));
      }
    });

    return request;
  }

  public static <I, O> StreamObserver<I> manyToMany(StreamObserver<O> response, String compression, BiConsumer<ReadStream<I>, WriteStream<O>> delegate) {
    trySetCompression(response, compression);
    StreamObserverReadStream<I> request = new StreamObserverReadStream<>();
    GrpcWriteStream<O> responseStream = new GrpcWriteStream<>(response);
    delegate.accept(request, responseStream);
    return request;
  }

  private static void trySetCompression(StreamObserver<?> response, String compression) {
    if (compression != null && response instanceof ServerCallStreamObserver<?>) {
      ServerCallStreamObserver<?> serverResponse = (ServerCallStreamObserver<?>) response;
      serverResponse.setCompression(compression);
    }
  }

  private static Throwable prepareError(Throwable throwable) {
    if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
      return throwable;
    } else {
      return Status.fromThrowable(throwable).asException();
    }
  }
}
