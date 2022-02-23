package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.io.IOException;
import java.io.InputStream;

public class GrpcService implements Handler<HttpServerRequest> {

  private Handler<GrpcRequest> requestHandler;
  private ServerServiceDefinition serviceDefinition;

  @Override
  public void handle(HttpServerRequest request) {
    String methodName = request.path().substring(1);
    ServerMethodDefinition<?, ?> method = serviceDefinition.getMethod(methodName);
    if (method != null) {
      MethodDescriptor desc = method.getMethodDescriptor();
      MethodDescriptor.MethodType type = desc.getType();
      switch (type) {
        case UNARY:
        case SERVER_STREAMING:
          GrpcRequest grpcRequest = new GrpcRequest(new GrpcResponseImpl(desc, request), method);
          request.handler(envelope -> {
            int len = envelope.getInt(1);
            Buffer data = envelope.slice(5, 5 + len);
            GrpcMessage msg = new GrpcMessage(data);
            Handler<GrpcMessage> msgHandler = grpcRequest.messageHandler;
            if (msgHandler != null) {
              msgHandler.handle(msg);
            }
          });
          request.endHandler(v -> {

          });
          handleUnary(grpcRequest);
          break;
        default:
          request.response().setStatusCode(500).end();
          break;
      }
    }
  }

  private void handleUnary(GrpcRequest request) {
    Handler<GrpcRequest> handler = requestHandler;
    if (handler != null) {
      if (handler != null) {
        handler.handle(request);
      }
    }
  }

  public GrpcService serviceDefinition(ServerServiceDefinition serviceDefinition) {
    this.serviceDefinition = serviceDefinition;
    return this;
  }

  public GrpcService requestHandler(Handler<GrpcRequest> requestHandler) {
    this.requestHandler = requestHandler;
    return this;
  }

  private static class GrpcResponseImpl implements GrpcResponse {

    private final MethodDescriptor desc;
    private final HttpServerRequest request;

    public GrpcResponseImpl(MethodDescriptor desc, HttpServerRequest request) {
      this.desc = desc;
      this.request = request;
    }

    @Override
    public void write(Object message) {
      write(message, false);
    }

    @Override
    public void end(Object message) {
      write(message, true);
    }

    private void write(Object message, boolean end) {
      InputStream stream = desc.streamResponse(message);
      byte[] tmp = new byte[256];
      int i;
      try {
        Buffer b = Buffer.buffer();
        b.appendByte((byte)0); // Compression
        b.appendIntLE(0); // Length
        while ((i = stream.read(tmp)) != -1) {
          b.appendBytes(tmp, 0, i);
        }
        b.setInt(1, b.length() - 5);
        MultiMap responseHeaders = request.response().headers();
        responseHeaders.set("content-type", "application/grpc");
        responseHeaders.set("grpc-encoding", "identity");
        responseHeaders.set("grpc-accept-encoding", "gzip");
        MultiMap responseTrailers = request.response().trailers();
        responseTrailers.set("grpc-status", "0");
        if (end) {
          request.response().end(b);
        } else {
          request.response().write(b);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
