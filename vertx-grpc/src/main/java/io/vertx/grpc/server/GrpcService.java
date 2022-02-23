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
        case CLIENT_STREAMING:
          GrpcRequest grpcRequest = new GrpcRequest(new GrpcResponseImpl(desc, request), method);
          request.handler(envelope -> {
            if (envelope.length() > 0) {
              int len = envelope.getInt(1);
              Buffer data = envelope.slice(5, 5 + len);
              GrpcMessage msg = new GrpcMessage(data);
              Handler<GrpcMessage> msgHandler = grpcRequest.messageHandler;
              if (msgHandler != null) {
                msgHandler.handle(msg);
              }
            } else {
              Handler<Void> handler = grpcRequest.endHandler;
              if (handler != null) {
                handler.handle(null);
              }
            }
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

    @Override
    public void end() {
      write(null, true);
    }

    private boolean headerSent;

    private void write(Object message, boolean end) {
      Buffer encoded;
      if (message != null) {
        InputStream stream = desc.streamResponse(message);
        byte[] tmp = new byte[256];
        int i;
        try {
          encoded = Buffer.buffer();
          encoded.appendByte((byte)0); // Compression
          encoded.appendIntLE(0); // Length
          while ((i = stream.read(tmp)) != -1) {
            encoded.appendBytes(tmp, 0, i);
          }
          encoded.setInt(1, encoded.length() - 5);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
      } else {
        encoded = null;
      }
      if (!headerSent) {
        headerSent = true;
        MultiMap responseHeaders = request.response().headers();
        responseHeaders.set("content-type", "application/grpc");
        responseHeaders.set("grpc-encoding", "identity");
        responseHeaders.set("grpc-accept-encoding", "gzip");
      }
      if (end) {
        MultiMap responseTrailers = request.response().trailers();
        responseTrailers.set("grpc-status", "0");
        if (encoded != null) {
          request.response().end(encoded);
        } else {
          request.response().end();
        }
      } else {
        request.response().write(encoded);
      }
    }
  }
}
