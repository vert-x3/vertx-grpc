package io.vertx.grpc.server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.streams.WriteStream;

public class GrpcClientRequest implements WriteStream<GrpcMessage> {

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

  @Override
  public GrpcClientRequest exceptionHandler(Handler<Throwable> handler) {
    httpRequest.exceptionHandler(handler);
    return this;
  }

  @Override
  public void write(GrpcMessage data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override
  public GrpcClientRequest setWriteQueueMaxSize(int maxSize) {
    httpRequest.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return httpRequest.writeQueueFull();
  }

  @Override
  public GrpcClientRequest drainHandler(Handler<Void> handler) {
    httpRequest.drainHandler(handler);
    return this;
  }

  public Future<Void> write(GrpcMessage message) {
    return write(message, false);
  }

  public Future<Void> end(GrpcMessage message) {
    return write(message, true);
  }

  public Future<Void> end() {
    if (!headerSent) {
      throw new IllegalStateException();
    }
    return httpRequest.end();
  }

  private Future<Void> write(GrpcMessage message, boolean end) {
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
      return httpRequest.end(message.encode());
    } else {
      return httpRequest.write(message.encode());
    }
  }

  public Future<GrpcClientResponse> response() {
    return response;
  }
}
