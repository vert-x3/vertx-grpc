package io.vertx.grpc.server;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class GrpcServerResponse {

  private final HttpServerResponse httpResponse;

  public GrpcServerResponse(HttpServerResponse httpResponse) {
    this.httpResponse = httpResponse;
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
      MultiMap responseHeaders = httpResponse.headers();
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", "identity");
      responseHeaders.set("grpc-accept-encoding", "gzip");
    }
    if (end) {
      MultiMap responseTrailers = httpResponse.trailers();
      responseTrailers.set("grpc-status", "0");
      if (encoded != null) {
        httpResponse.end(encoded);
      } else {
        httpResponse.end();
      }
    } else {
      httpResponse.write(encoded);
    }
  }
}
