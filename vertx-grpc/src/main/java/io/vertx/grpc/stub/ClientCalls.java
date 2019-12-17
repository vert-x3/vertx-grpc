package io.vertx.grpc.stub;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Rogelio Orts
 * @author Eduard Català
 */
public final class ClientCalls {

  private ClientCalls() {
  }

  public static <I, O> Future<O> oneToOne(I request, BiConsumer<I, StreamObserver<O>> delegate) {
    Promise<O> promise = Promise.promise();
    delegate.accept(request, toStreamObserver(promise));
    return promise.future();
  }

  public static <I, O> ReadStream<O> oneToMany(I request, BiConsumer<I, StreamObserver<O>> delegate) {
    StreamObserverReadStream<O> response = new StreamObserverReadStream<>();
    delegate.accept(request, response);
    return response;
  }

  public static <I, O> Future<O> manyToOne(Handler<WriteStream<I>> requestHandler, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
    Promise<O> promise = Promise.promise();
    StreamObserver<I> request = delegate.apply(toStreamObserver(promise));
    requestHandler.handle(new GrpcWriteStream(request));
    return promise.future();
  }

  public static <I, O> ReadStream<O> manyToMany(Handler<WriteStream<I>> requestHandler, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
    StreamObserverReadStream<O> response = new StreamObserverReadStream<>();
    StreamObserver<I> request = delegate.apply(response);
    requestHandler.handle(new GrpcWriteStream(request));
    return response;
  }

  private static <O> StreamObserver<O> toStreamObserver(Promise<O> promise) {
    return new StreamObserver<O>() {
      @Override
      public void onNext(O tResponse) {
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

}
