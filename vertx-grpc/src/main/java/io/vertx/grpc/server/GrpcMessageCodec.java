package io.vertx.grpc.server;

import io.vertx.core.buffer.Buffer;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class GrpcMessageCodec {

  public static Iterable<GrpcMessage> decode(Buffer envelope) {
    return () -> new Iterator<GrpcMessage>() {
      int idx = 0;
      @Override
      public boolean hasNext() {
        return idx < envelope.length();
      }
      @Override
      public GrpcMessage next() {
        if (idx == envelope.length()) {
          throw new NoSuchElementException();
        }
        int len = envelope.getInt(idx + 1);
        Buffer data = envelope.slice(idx + 5, idx + 5 + len);
        GrpcMessage msg = new GrpcMessage(data);
        idx += 5 + len;
        return msg;
      }
    };
  }
}
