package io.vertx.grpc.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public class GrpcServerRequest {

  final HttpServerRequest httpRequest;
  Handler<GrpcMessage> messageHandler;
  Handler<Void> endHandler;

  public GrpcServerRequest(HttpServerRequest httpRequest) {
    this.httpRequest = httpRequest;
  }

  public String fullMethodName() {
    return httpRequest.path().substring(1);
  }

  public GrpcServerRequest messageHandler(Handler<GrpcMessage> messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public GrpcServerRequest endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  public void write(Buffer message) {
    write(message, false);
  }

  public void end(Buffer message) {
    write(message, true);
  }

  public void end() {
    write(null, true);
  }

  private boolean headerSent;

  private void write(Buffer message, boolean end) {
    Buffer encoded;
    if (message != null) {
      encoded = Buffer.buffer(message.length());
      encoded.appendByte((byte)0); // Compression
      encoded.appendInt(message.length()); // Length
      encoded.appendBuffer(message);
    } else {
      encoded = null;
    }
    if (!headerSent) {
      headerSent = true;
      MultiMap responseHeaders = httpRequest.response().headers();
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", "identity");
      responseHeaders.set("grpc-accept-encoding", "gzip");
    }
    if (end) {
      MultiMap responseTrailers = httpRequest.response().trailers();
      responseTrailers.set("grpc-status", "0");
      if (encoded != null) {
        httpRequest.response().end(encoded);
      } else {
        httpRequest.response().end();
      }
    } else {
      httpRequest.response().write(encoded);
    }
  }
}
