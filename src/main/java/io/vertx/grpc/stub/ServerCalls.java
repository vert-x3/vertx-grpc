package io.vertx.grpc.stub;

import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.function.Function;

public final class ServerCalls {

  private ServerCalls() {}

  public static <TRequest, TResponse> void oneToOne(
    final TRequest request,
    final StreamObserver<TResponse> responseObserver,
    final Function<Future<TRequest>, Future<TResponse>> delegate) {
    try {
      final Future<TRequest> futureRequest = Promise.succeededPromise(request).future();

      final Future<TResponse> futureResponse = Preconditions.checkNotNull(delegate.apply(futureRequest));
      futureResponse.setHandler(res -> {
        if (res.succeeded()) {
          responseObserver.onNext(res.result());
          responseObserver.onCompleted();
        } else {
          responseObserver.onError(prepareError(res.cause()));
        }
      });
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }
  }

  public static <TRequest, TResponse> void oneToMany(
    final TRequest request,
    final StreamObserver<TResponse> responseObserver,
    final Function<Future<TRequest>, ReadStream<TResponse>> delegate) {
    try {
      final Future<TRequest> futureRequest = Promise.succeededPromise(request).future();

      final ReadStream<TResponse> readStreamResponse = Preconditions.checkNotNull(delegate.apply(futureRequest));
      readStreamResponse.pause();
      readStreamResponse.handler(responseObserver::onNext);
      readStreamResponse.endHandler(v -> responseObserver.onCompleted());
      readStreamResponse.exceptionHandler(e -> responseObserver.onError(prepareError(e)));
      readStreamResponse.resume();
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }
  }

  public static <TRequest, TResponse> StreamObserver<TRequest> manyToOne(
    final StreamObserver<TResponse> responseObserver,
    final Function<ReadStream<TRequest>, Future<TResponse>> delegate) {
    final RxServerStreamObserverAndPublisher<TRequest> streamObserverPublisher =
      new RxServerStreamObserverAndPublisher<TRequest>((ServerCallStreamObserver<TResponse>) responseObserver, null);

    try {
      final Single<TResponse> rxResponse = Preconditions.checkNotNull(delegate.apply(Flowable.fromPublisher(streamObserverPublisher)));
      rxResponse.subscribe(
        new Consumer<TResponse>() {
          @Override
          public void accept(TResponse value) {
            // Don't try to respond if the server has already canceled the request
            if (!streamObserverPublisher.isCancelled()) {
              responseObserver.onNext(value);
              responseObserver.onCompleted();
            }
          }
        },
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) {
            // Don't try to respond if the server has already canceled the request
            if (!streamObserverPublisher.isCancelled()) {
              streamObserverPublisher.abortPendingCancel();
              responseObserver.onError(prepareError(throwable));
            }
          }
        }
      );
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }

    return streamObserverPublisher;
  }

  public static <TRequest, TResponse> StreamObserver<TRequest> manyToMany(
    final StreamObserver<TResponse> responseObserver,
    final Function<Flowable<TRequest>, Flowable<TResponse>> delegate) {
    final RxServerStreamObserverAndPublisher<TRequest> streamObserverPublisher =
      new RxServerStreamObserverAndPublisher<TRequest>((ServerCallStreamObserver<TResponse>) responseObserver, null);

    try {
      final Flowable<TResponse> rxResponse = Preconditions.checkNotNull(delegate.apply(Flowable.fromPublisher(streamObserverPublisher)));
      final RxSubscriberAndServerProducer<TResponse> subscriber = new RxSubscriberAndServerProducer<TResponse>();
      subscriber.subscribe((ServerCallStreamObserver<TResponse>) responseObserver);
      // Don't try to respond if the server has already canceled the request
      rxResponse.subscribe(subscriber);
    } catch (Throwable throwable) {
      responseObserver.onError(prepareError(throwable));
    }

    return streamObserverPublisher;
  }

  private static Throwable prepareError(Throwable throwable) {
    if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
      return throwable;
    } else {
      return Status.fromThrowable(throwable).asException();
    }
  }

}
