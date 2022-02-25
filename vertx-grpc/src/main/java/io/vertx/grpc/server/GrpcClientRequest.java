package io.vertx.grpc.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

public class GrpcClientRequest {

  private final HttpClientRequest httpRequest;
  private String fullMethodName;
  private boolean headerSent;
  private Future<GrpcClientResponse> response;

  public GrpcClientRequest(HttpClientRequest httpRequest) {
    this.httpRequest = httpRequest;
    this.response = httpRequest.response().map(httpResponse -> {
      GrpcClientResponse grpcResponse = new GrpcClientResponse(httpResponse);
      grpcResponse.init();
      return grpcResponse;
    });
  }

  public GrpcClientRequest fullMethodName(String fullMethodName) {
    this.fullMethodName = fullMethodName;
    return this;
  }

  public void write(GrpcMessage message) {
    write(message, false);
  }

  public void end(GrpcMessage message) {
    write(message, true);
  }

  public void end() {
    if (!headerSent) {
      throw new IllegalStateException();
    }
    httpRequest.end();
  }

  private void write(GrpcMessage message, boolean end) {
    if (!headerSent) {
      if (fullMethodName == null) {
        throw new IllegalStateException();
      }
      httpRequest.putHeader("content-type", "application/grpc");
      httpRequest.putHeader("grpc-encoding", "identity");
      httpRequest.putHeader("grpc-accept-encoding", "gzip");
      httpRequest.putHeader("grpc-accept-encoding", "gzip");
      httpRequest.putHeader("te", "trailers");
      httpRequest.setChunked(true);
      httpRequest.setURI("/" + fullMethodName);
      headerSent = true;
    }
    if (end) {
      httpRequest.end(message.encode());
    } else {
      httpRequest.write(message.encode());
    }
  }

  public Future<GrpcClientResponse> response() {
    return response;
  }
}
