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
          GrpcRequest grpcRequest = new GrpcRequest(new GrpcResponse() {
            @Override
            public void write(Object message) {
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
                request.response().end(b);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }, method);
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
            // Totod
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
}
