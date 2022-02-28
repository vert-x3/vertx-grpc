package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.streams.ReadStream;

public class GrpcServerRequest extends GrpcMessageDecoder implements ReadStream<GrpcMessage> {

  final HttpServerRequest httpRequest;
  final GrpcServerResponse response;
  private Handler<GrpcMessage> messageHandler;
  private Handler<Void> endHandler;

  public GrpcServerRequest(HttpServerRequest httpRequest) {
    super(((HttpServerRequestInternal) httpRequest).context(), httpRequest);
    this.httpRequest = httpRequest;
    this.response = new GrpcServerResponse(httpRequest.response());
  }

  public String fullMethodName() {
    return httpRequest.path().substring(1);
  }

  protected void handleMessage(GrpcMessage message) {
    Handler<GrpcMessage> msgHandler = messageHandler;
    if (msgHandler != null) {
      msgHandler.handle(message);
    }
  }

  protected void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  @Override
  public GrpcServerRequest handler(Handler<GrpcMessage> handler) {
    return messageHandler(handler);
  }

  public GrpcServerRequest messageHandler(Handler<GrpcMessage> messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public GrpcServerRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  @Override
  public GrpcServerRequest exceptionHandler(Handler<Throwable> handler) {
    httpRequest.exceptionHandler(handler);
    return this;
  }

  public GrpcServerResponse response() {
    return response;
  }

  @Override
  public GrpcServerRequest pause() {
    return (GrpcServerRequest) super.pause();
  }

  @Override
  public GrpcServerRequest resume() {
    return (GrpcServerRequest) super.resume();
  }

  @Override
  public GrpcServerRequest fetch(long amount) {
    return (GrpcServerRequest) super.fetch(amount);
  }
}
