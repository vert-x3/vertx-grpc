package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;

public class GrpcClientResponse extends GrpcMessageDecoder {

  private HttpClientResponse httpResponse;
  private Handler<GrpcMessage> messageHandler;
  private Handler<Void> endHandler;

  public GrpcClientResponse(HttpClientResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  void init() {
    httpResponse.handler(this);
    httpResponse.endHandler(v -> {
      Handler<Void> handler = endHandler;
      if (handler != null) {
        handler.handle(null);
      }
    });
  }

  @Override
  public void handle(GrpcMessage msg) {
    Handler<GrpcMessage> handler = messageHandler;
    if (handler != null) {
      handler.handle(msg);
    }
  }

  public GrpcClientResponse handler(Handler<GrpcMessage> handler) {
    messageHandler = handler;
    return this;
  }

  public GrpcClientResponse endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }
}
