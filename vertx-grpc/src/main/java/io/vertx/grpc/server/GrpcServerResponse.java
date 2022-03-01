package io.vertx.grpc.server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;

public class GrpcServerResponse implements WriteStream<GrpcMessage> {

  private final HttpServerResponse httpResponse;
  private String encoding = "identity";

  public GrpcServerResponse(HttpServerResponse httpResponse) {
    this.httpResponse = httpResponse;
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

  private boolean headerSent;

  private Future<Void>  write(GrpcMessage message, boolean end) {
    if (!headerSent) {
      headerSent = true;
      MultiMap responseHeaders = httpResponse.headers();
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", encoding);
      responseHeaders.set("grpc-accept-encoding", "gzip");
    }
    if (end) {
      MultiMap responseTrailers = httpResponse.trailers();
      responseTrailers.set("grpc-status", "0");
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
