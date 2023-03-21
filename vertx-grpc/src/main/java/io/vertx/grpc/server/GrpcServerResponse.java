package io.vertx.grpc.server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;

import java.util.Objects;

public class GrpcServerResponse implements WriteStream<GrpcMessage> {

  private final HttpServerResponse httpResponse;
  private String encoding = "identity";
  private GrpcStatus status = GrpcStatus.OK;
  private boolean headerSent;

  public GrpcServerResponse(HttpServerResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  public GrpcServerResponse status(GrpcStatus status) {
    Objects.requireNonNull(status);
    this.status = status;
    return this;
  }

  public GrpcServerResponse encoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  @Override
  public WriteStream<GrpcMessage> exceptionHandler(Handler<Throwable> handler) {
    httpResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public Future<Void> write(GrpcMessage data) {
    return write(data, false);
  }

  @Override
  public void write(GrpcMessage data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  public Future<Void> end() {
    return write(null, true);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override
  public GrpcServerResponse setWriteQueueMaxSize(int maxSize) {
    httpResponse.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return httpResponse.writeQueueFull();
  }

  @Override
  public GrpcServerResponse drainHandler(Handler<Void> handler) {
    httpResponse.drainHandler(handler);
    return this;
  }

  private Future<Void>  write(GrpcMessage message, boolean end) {
    MultiMap responseHeaders = httpResponse.headers();
    if (!headerSent) {
      headerSent = true;
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", encoding);
      responseHeaders.set("grpc-accept-encoding", "gzip");
      if (end) {
        responseHeaders.set("grpc-status", status.toString());
      }
    }
    if (end) {
      if (!responseHeaders.contains("grpc-status")) {
        MultiMap responseTrailers = httpResponse.trailers();
        responseTrailers.set("grpc-status", status.toString());
      }
      if (message != null) {
        return httpResponse.end(message.encode(encoding));
      } else {
        return httpResponse.end();
      }
    } else {
      return httpResponse.write(message.encode(encoding));
    }
  }
}
