package io.vertx.grpc;

import io.grpc.stub.StreamObserver;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.impl.GrpcWriteStreamImpl;

public interface GrpcWriteStream<T> extends WriteStream<T> {

  static <T> GrpcWriteStream<T> create(StreamObserver<T> observer) {
    return new GrpcWriteStreamImpl<T>(observer);
  }

  /**
   * Set an exception handler on the write stream.
   *
   * @param handler the exception handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  GrpcWriteStream<T> exceptionHandler(Handler<Throwable> handler);

  /**
   * Write some data to the stream. The data is put on an internal write queue, and the write actually happens
   * asynchronously. To avoid running out of memory by putting too much on the write queue,
   * check the {@link #writeQueueFull} method before writing. This is done automatically if using a {@link Pump}.
   *
   * @param data the data to write
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcWriteStream<T> write(T data);

  /**
   * Set the maximum size of the write queue to {@code maxSize}. You will still be able to write to the stream even
   * if there is more than {@code maxSize} items in the write queue. This is used as an indicator by classes such as
   * {@code Pump} to provide flow control.
   * <p/>
   * The value is defined by the implementation of the stream, e.g in bytes for a
   * {@link io.vertx.core.net.NetSocket}, the number of {@link io.vertx.core.eventbus.Message} for a
   * {@link io.vertx.core.eventbus.MessageProducer}, etc...
   *
   * @param maxSize the max size of the write stream
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcWriteStream<T> setWriteQueueMaxSize(int maxSize);

  /**
   * Set a drain handler on the stream. If the write queue is full, then the handler will be called when the write
   * queue is ready to accept buffers again. See {@link Pump} for an example of this being used.
   * <p/>
   * The stream implementation defines when the drain handler, for example it could be when the queue size has been
   * reduced to {@code maxSize / 2}.
   *
   * @param handler the handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcWriteStream<T> drainHandler(@Nullable Handler<Void> handler);

  /**
   * Send an error event into the stream.
   *
   * @param t any error
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcWriteStream<T> fail(Throwable t);

  /**
   * Should not be used by end user, it is a simple accessor the the underlying gRPC StreamObserver.
   *
   * @return the underlying stream observer.
   */
  @GenIgnore
  StreamObserver<T> writeObserver();
}
