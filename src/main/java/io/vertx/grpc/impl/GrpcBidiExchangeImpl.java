package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.GrpcBidiExchange;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcWriteStream;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcBidiExchangeImpl<I,O> implements GrpcBidiExchange<I,O> {

  private final GrpcReadStream<I> readStream;
  private final GrpcWriteStream<O> writeStream;

  public GrpcBidiExchangeImpl(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    this.readStream = readStream;
    writeStream = GrpcWriteStream.create(writeObserver);
  }

  public GrpcBidiExchangeImpl(StreamObserver<I> readObserver, StreamObserver<O> writeObserver) {
    readStream = GrpcReadStream.create(readObserver);
    writeStream = GrpcWriteStream.create(writeObserver);
  }

  @Override
  public GrpcBidiExchange<I, O> exceptionHandler(Handler<Throwable> handler) {
    readStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> write(O data) {
    writeStream.write(data);
    return this;
  }

  @Override
  public WriteStream<O> write(O data, Handler<AsyncResult<Void>> handler) {
    writeStream.write(data, handler);
    return this;
  }

  @Override
  public void end() {
    writeStream.end();
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    writeStream.end(handler);
  }

  @Override
  public GrpcBidiExchange<I, O> setWriteQueueMaxSize(int maxSize) {
    writeStream.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return writeStream.writeQueueFull();
  }

  @Override
  public GrpcBidiExchange<I, O> drainHandler(Handler<Void> handler) {
    writeStream.drainHandler(handler);
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> fail(Throwable t) {
    writeStream.fail(t);
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> handler(Handler<I> handler) {
    readStream.handler(handler);
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> pause() {
    readStream.pause();
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> resume() {
    readStream.resume();
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> fetch(long amount) {
    readStream.fetch(amount);
    return this;
  }

  @Override
  public GrpcBidiExchange<I, O> endHandler(Handler<Void> handler) {
    readStream.endHandler(handler);
    return this;
  }

  @Override
  public StreamObserver<I> readObserver() {
    return readStream.readObserver();
  }

  @Override
  public GrpcBidiExchange<I, O> setReadObserver(StreamObserver<I> observer) {
    readStream.setReadObserver(observer);
    return this;
  }

  @Override
  public StreamObserver<O> writeObserver() {
    return writeStream.writeObserver();
  }
}
