package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class GrpcServerRequest {

  final HttpServerRequest httpRequest;
  final GrpcServerResponse response;
  private Handler<GrpcMessage> messageHandler;
  private Handler<Void> endHandler;

  public GrpcServerRequest(HttpServerRequest httpRequest) {
    this.httpRequest = httpRequest;
    this.response = new GrpcServerResponse(httpRequest.response());
  }

  public String fullMethodName() {
    return httpRequest.path().substring(1);
  }

  void handleMessage(GrpcMessage message) {
    Handler<GrpcMessage> msgHandler = messageHandler;
    if (msgHandler != null) {
      msgHandler.handle(message);
    }
  }

  void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  public GrpcServerRequest messageHandler(Handler<GrpcMessage> messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public GrpcServerRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public GrpcServerResponse response() {
    return response;
  }
}
