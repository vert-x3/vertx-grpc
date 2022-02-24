package io.vertx.grpc.server;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

public class GrpcClientRequest {

  private final HttpClientRequest request;
  private String fullMethodName;
  private boolean headerSent;
  private Future<GrpcClientResponse> response;

  public GrpcClientRequest(HttpClientRequest httpRequest) {
    this.request = httpRequest;
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

  private void write(GrpcMessage message, boolean end) {
    if (!headerSent) {
      if (fullMethodName == null) {
        throw new IllegalStateException();
      }
      request.putHeader("content-type", "application/grpc");
      request.putHeader("grpc-encoding", "identity");
      request.putHeader("grpc-accept-encoding", "gzip");
      request.putHeader("grpc-accept-encoding", "gzip");
      request.putHeader("te", "trailers");
      request.setChunked(true);
      request.setURI("/" + fullMethodName);
    }
    if (end) {
      request.end(message.encode());
    } else {
      request.write(message.encode());
    }
  }

  public Future<GrpcClientResponse> response() {
    return response;
  }
}
