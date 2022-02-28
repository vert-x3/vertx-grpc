package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.streams.ReadStream;

public class GrpcClientResponse extends GrpcMessageDecoder implements ReadStream<GrpcMessage> {

  private HttpClientResponse httpResponse;
  private Handler<GrpcMessage> messageHandler;
  private Handler<Void> endHandler;

  public GrpcClientResponse(HttpClientResponse httpResponse) {
    super(Vertx.currentContext(), httpResponse); // A bit ugly
    this.httpResponse = httpResponse;
  }

  @Override
  protected void handleMessage(GrpcMessage msg) {
    Handler<GrpcMessage> handler = messageHandler;
    if (handler != null) {
      handler.handle(msg);
    }
  }

  protected void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  public GrpcClientResponse messageHandler(Handler<GrpcMessage> handler) {
    messageHandler = handler;
    return this;
  }

  @Override
  public GrpcClientResponse exceptionHandler(Handler<Throwable> handler) {
    httpResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcClientResponse handler(Handler<GrpcMessage> handler) {
    return messageHandler(handler);
  }

  public GrpcClientResponse endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  @Override
  public GrpcClientResponse pause() {
    return (GrpcClientResponse) super.pause();
  }

  @Override
  public GrpcClientResponse resume() {
    return (GrpcClientResponse) super.resume();
  }

  @Override
  public GrpcClientResponse fetch(long amount) {
    return (GrpcClientResponse) super.fetch(amount);
  }
}
