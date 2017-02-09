package io.vertx.grpc.impl;

import io.grpc.stub.StreamObserver;
import io.vertx.core.Handler;
import io.vertx.grpc.GrpcExchange;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcWriteStream;

public class GrpcExchangeImpl<I,O> implements GrpcExchange<I,O> {

  private final GrpcReadStream<I> readStream;
  private final GrpcWriteStream<O> writeStream;

  public GrpcExchangeImpl(GrpcReadStream<I> readStream, StreamObserver<O> writeObserver) {
    this.readStream = readStream;
    writeStream = GrpcWriteStream.create(writeObserver);
  }

  public GrpcExchangeImpl(StreamObserver<I> readObserver, StreamObserver<O> writeObserver) {
    readStream = GrpcReadStream.create(readObserver);
    writeStream = GrpcWriteStream.create(writeObserver);
  }

  @Override
  public GrpcExchange<I, O> exceptionHandler(Handler<Throwable> handler) {
    readStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcExchange<I, O> write(O data) {
    writeStream.write(data);
    return this;
  }

  @Override
  public void end() {
    writeStream.end();
  }

  @Override
  public GrpcExchange<I, O> setWriteQueueMaxSize(int maxSize) {
    writeStream.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return writeStream.writeQueueFull();
  }

  @Override
  public GrpcExchange<I, O> drainHandler(Handler<Void> handler) {
    writeStream.drainHandler(handler);
    return this;
  }

  @Override
  public GrpcExchange<I, O> fail(Throwable t) {
    writeStream.fail(t);
    return this;
  }

  @Override
  public GrpcExchange<I, O> handler(Handler<I> handler) {
    readStream.handler(handler);
    return this;
  }

  @Override
  public GrpcExchange<I, O> pause() {
    readStream.pause();
    return this;
  }

  @Override
  public GrpcExchange<I, O> resume() {
    readStream.resume();
    return this;
  }

  @Override
  public GrpcExchange<I, O> endHandler(Handler<Void> handler) {
    readStream.endHandler(handler);
    return this;
  }

  @Override
  public StreamObserver<I> readObserver() {
    return readStream.readObserver();
  }

  @Override
  public StreamObserver<O> writeObserver() {
    return writeStream.writeObserver();
  }
}
