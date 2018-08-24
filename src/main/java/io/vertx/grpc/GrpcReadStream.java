package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.grpc.impl.GrpcReadStreamImpl;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public interface GrpcReadStream<T> extends ReadStream<T> {

  static <T> GrpcReadStream<T> create(StreamObserver<T> observer) {
    return new GrpcReadStreamImpl<T>(observer);
  }

  static <T> GrpcReadStream<T> create() {
    return new GrpcReadStreamImpl<T>();
  }

  /**
   * Set an exception handler on the read stream.
   *
   * @param handler the exception handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> exceptionHandler(Handler<Throwable> handler);

  /**
   * Set a data handler. As data is read, the handler will be called with the data.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> handler(@Nullable Handler<T> handler);

  /**
   * Pause the {@code ReadSupport}. While it's paused, no data will be sent to the {@code dataHandler}
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> pause();

  /**
   * Resume reading. If the {@code ReadSupport} has been paused, reading will recommence on it.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> resume();

  /**
   * Fetch the specified {@code amount} of elements. If the {@code ReadStream} has been paused, reading will
   * recommence with the specified {@code amount} of items, otherwise the specified {@code amount} will
   * be added to the current stream demand.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> fetch(long amount);

  /**
   * Set an end handler. Once the stream has ended, and there is no more data to be read, this handler will be called.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  @Fluent
  GrpcReadStream<T> endHandler(@Nullable Handler<Void> endHandler);

  /**
   * Should not be used by end user, it is a simple accessor the the underlying gRPC StreamObserver.
   *
   * @return the underlying stream observer.
   */
  @GenIgnore
  StreamObserver<T> readObserver();

  @GenIgnore
  GrpcReadStream<T> setReadObserver(StreamObserver<T> observer);
}
