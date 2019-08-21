package io.vertx.grpc.stub;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.function.BiConsumer;


/**
 * @author Rogelio Orts
 * @author Eduard Català
 */
public final class ServerCalls {

  private ServerCalls() {
  }

  public static <I, O> void oneToOne(I request, StreamObserver<O> response, BiConsumer<I, Promise<O>> delegate) {
    try {
      Promise<O> promise = Promise.promise();
      delegate.accept(request, promise);

      promise.future().setHandler(res -> {
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

  public static <I, O> void oneToMany(I request, StreamObserver<O> response, BiConsumer<I, WriteStream<O>> delegate) {
    try {
      GrpcWriteStream<O> responseWriteStream = new GrpcWriteStream<>(response);
      delegate.accept(request, responseWriteStream);
    } catch (Throwable throwable) {
      response.onError(prepareError(throwable));
    }
  }

  public static <I, O> StreamObserver<I> manyToOne(StreamObserver<O> response, BiConsumer<ReadStream<I>, Promise<O>> delegate) {
    Promise<O> promise = Promise.promise();
    StreamObserverReadStream<I> request = new StreamObserverReadStream<>();
    delegate.accept(request, promise);
    promise.future().setHandler(res -> {
      if (res.succeeded()) {
        response.onNext(res.result());
        response.onCompleted();
      } else {
        response.onError(prepareError(res.cause()));
      }
    });

    return request;
  }

  public static <I, O> StreamObserver<I> manyToMany(StreamObserver<O> response, BiConsumer<ReadStream<I>, WriteStream<O>> delegate) {
    StreamObserverReadStream<I> request = new StreamObserverReadStream<>();
    GrpcWriteStream<O> responseStream = new GrpcWriteStream<>(response);
    delegate.accept(request, responseStream);
    return request;
  }

  private static Throwable prepareError(Throwable throwable) {
    if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
      return throwable;
    } else {
      return Status.fromThrowable(throwable).asException();
    }
  }

}
