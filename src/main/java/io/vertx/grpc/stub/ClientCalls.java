package io.vertx.grpc.stub;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ClientCalls {

  private ClientCalls() {
  }

  public static <TRequest, TResponse> Future<TResponse> oneToOne(
      Future<TRequest> futureSource,
      BiConsumer<TRequest, StreamObserver<TResponse>> delegate) {
    Promise<TResponse> promise = Promise.promise();

    futureSource.setHandler(res -> {
      if (res.succeeded()) {
        delegate.accept(res.result(), receiveOne(promise));
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  public static <TRequest, TResponse> ReadStream<TResponse> oneToMany(
      Future<TRequest> futureSource,
      BiConsumer<TRequest, StreamObserver<TResponse>> delegate) {
    StreamObserverReadStream<TResponse> readStream = new StreamObserverReadStream<>();

    futureSource.setHandler(res -> {
      if (res.succeeded()) {
        delegate.accept(res.result(), readStream);
      } else {
        readStream.onError(res.cause());
      }
    });

    return readStream;
  }

  public static <TRequest, TResponse> Future<TResponse> manyToOne(
      ReadStream<TRequest> readStreamSource,
      Function<StreamObserver<TResponse>, StreamObserver<TRequest>> delegate) {
    readStreamSource.pause();
    Promise<TResponse> promise = Promise.promise();
    StreamObserver<TRequest> streamObserverRequest = delegate.apply(receiveOne(promise));
    receiveMany(readStreamSource, streamObserverRequest);

    return promise.future();
  }

  public static <TRequest, TResponse> ReadStream<TResponse> manyToMany(
      ReadStream<TRequest> readStreamSource,
      Function<StreamObserver<TResponse>, StreamObserver<TRequest>> delegate) {
    readStreamSource.pause();
    StreamObserverReadStream<TResponse> streamObserverResponse = new StreamObserverReadStream<>();
    StreamObserver<TRequest> streamObserverRequest = delegate.apply(streamObserverResponse);
    receiveMany(readStreamSource, streamObserverRequest);

    return streamObserverResponse;
  }

  private static <TResponse> StreamObserver<TResponse> receiveOne(Promise<TResponse> promise) {
    return new StreamObserver<TResponse>() {
      @Override
      public void onNext(TResponse tResponse) {
        promise.complete(tResponse);
      }

      @Override
      public void onError(Throwable throwable) {
        promise.fail(throwable);
      }

      @Override
      public void onCompleted() {
        // Do nothing
      }
    };
  }

  private static <TRequest> void receiveMany(ReadStream<TRequest> readStreamSource, StreamObserver<TRequest> streamObserverRequest) {
    readStreamSource.endHandler(v -> streamObserverRequest.onCompleted());
    readStreamSource.exceptionHandler(streamObserverRequest::onError);
    readStreamSource.handler(streamObserverRequest::onNext);
    readStreamSource.resume();
  }

}
