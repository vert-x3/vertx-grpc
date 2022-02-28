package io.vertx.grpc.server;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;

public class GrpcServerResponse implements WriteStream<GrpcMessage> {

  private final HttpServerResponse httpResponse;

  public GrpcServerResponse(HttpServerResponse httpResponse) {
    this.httpResponse = httpResponse;
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
    Buffer encoded;
    if (message != null) {
      encoded = Buffer.buffer(message.data().length());
      encoded.appendByte((byte)0); // Compression
      encoded.appendInt(message.data().length()); // Length
      encoded.appendBuffer(message.data());
    } else {
      encoded = null;
    }
    if (!headerSent) {
      headerSent = true;
      MultiMap responseHeaders = httpResponse.headers();
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", "identity");
      responseHeaders.set("grpc-accept-encoding", "gzip");
    }
    if (end) {
      MultiMap responseTrailers = httpResponse.trailers();
      responseTrailers.set("grpc-status", "0");
      if (encoded != null) {
        return httpResponse.end(encoded);
      } else {
        return httpResponse.end();
      }
    } else {
      return httpResponse.write(encoded);
    }
  }
}
