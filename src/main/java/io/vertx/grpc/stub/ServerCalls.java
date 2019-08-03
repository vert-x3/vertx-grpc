package io.vertx.grpc.stub;

import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;

import java.util.function.Function;

public final class ServerCalls {

  private ServerCalls() {
  }

  public static <TRequest, TResponse> void oneToOne(
      TRequest request, StreamObserver<TResponse> responseObserver,
      Function<Future<TRequest>, Future<TResponse>> delegate) {
    try {
      Future<TRequest> futureRequest = Promise.succeededPromise(request).future();

      Future<TResponse> futureResponse = Preconditions.checkNotNull(delegate.apply(futureRequest));
      sendOne(responseObserver, futureResponse);
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }
  }

  public static <TRequest, TResponse> void oneToMany(
      TRequest request, StreamObserver<TResponse> responseObserver,
      Function<Future<TRequest>, ReadStream<TResponse>> delegate) {
    try {
      Future<TRequest> futureRequest = Promise.succeededPromise(request).future();

      ReadStream<TResponse> readStreamResponse = Preconditions.checkNotNull(delegate.apply(futureRequest));
      sendMany(responseObserver, readStreamResponse);
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }
  }

  public static <TRequest, TResponse> StreamObserver<TRequest> manyToOne(
      StreamObserver<TResponse> responseObserver, Function<ReadStream<TRequest>, Future<TResponse>> delegate) {
      StreamObserverReadStream<TRequest> requestStreamReader = new StreamObserverReadStream<>();
    Future<TResponse> readStreamResponse = Preconditions.checkNotNull(delegate.apply(requestStreamReader));
    sendOne(responseObserver, readStreamResponse);

    return requestStreamReader;
  }

  public static <TRequest, TResponse> StreamObserver<TRequest> manyToMany(
      StreamObserver<TResponse> responseObserver, Function<ReadStream<TRequest>, ReadStream<TResponse>> delegate) {
      StreamObserverReadStream<TRequest> requestStreamReader = new StreamObserverReadStream<>();
    ReadStream<TResponse> readStreamResponse = Preconditions.checkNotNull(delegate.apply(requestStreamReader));
    sendMany(responseObserver, readStreamResponse);

    return requestStreamReader;
  }

  private static <TResponse> void sendOne(StreamObserver<TResponse> responseObserver, Future<TResponse> futureResponse) {
    futureResponse.setHandler(res -> {
      if (res.succeeded()) {
        responseObserver.onNext(res.result());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(prepareError(res.cause()));
      }
    });
  }

  private static <TResponse> void sendMany(StreamObserver<TResponse> responseObserver, ReadStream<TResponse> readStreamResponse) {
    readStreamResponse.endHandler(v -> responseObserver.onCompleted());
    readStreamResponse.exceptionHandler(e -> responseObserver.onError(prepareError(e)));
    readStreamResponse.handler(responseObserver::onNext);
    readStreamResponse.resume();
  }

  private static Throwable prepareError(Throwable throwable) {
    if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
      return throwable;
    } else {
      return Status.fromThrowable(throwable).asException();
    }
  }

}
