package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface StreamHelper {

  /**
   *
   * @param handler
   * @param <T>
   * @return
   */
  static <T> StreamObserver<T> future(Handler<AsyncResult<T>> handler) {
    Future<T> fut = Future.future();
    fut.setHandler(handler);
    return new StreamObserver<T>() {
      T val;
      @Override
      public void onNext(T value) {
        val = value;
      }

      @Override
      public void onError(Throwable t) {
        fut.fail(t);
      }

      @Override
      public void onCompleted() {
        fut.complete(val);
      }
    };
  }
}
