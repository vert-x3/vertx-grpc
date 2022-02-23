package io.vertx.grpc.server;

import io.grpc.ServerMethodDefinition;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class GrpcService implements Handler<HttpServerRequest> {

  private Handler<GrpcRequest> requestHandler;
  private Map<String, MethodCallHandler<?, ?>> handlerMap = new HashMap<>();

  @Override
  public void handle(HttpServerRequest request) {
    GrpcRequest grpcRequest = new GrpcRequest(request);
    request.handler(envelope -> {
      int idx = 0;
      while (idx < envelope.length()) {
        int len = envelope.getInt(idx + 1);
        Buffer data = envelope.slice(idx + 5, idx + 5 + len);
        GrpcMessage msg = new GrpcMessage(data);
        Handler<GrpcMessage> msgHandler = grpcRequest.messageHandler;
        if (msgHandler != null) {
          msgHandler.handle(msg);
        }
        idx += 5 + len;
      }
    });
    request.endHandler(v -> {
      Handler<Void> handler = grpcRequest.endHandler;
      if (handler != null) {
        handler.handle(null);
      }
    });
    handleUnary(grpcRequest);
  }

  private void handleUnary(GrpcRequest request) {
    String fmn = request.fullMethodName();
    MethodCallHandler<?, ?> method = handlerMap.get(fmn);
    if (method != null) {
      method.handle(request);
    } else {
      Handler<GrpcRequest> handler = requestHandler;
      if (handler != null) {
        handler.handle(request);
      } else {
        request.httpRequest.response().setStatusCode(500).end();
      }
    }
  }

  public GrpcService requestHandler(Handler<GrpcRequest> requestHandler) {
    this.requestHandler = requestHandler;
    return this;
  }

  public <Req, Resp> GrpcService methodHandler(ServerMethodDefinition<Req, Resp> def, Handler<GrpcMethodCall<Req, Resp>> handler) {
    handlerMap.put(def.getMethodDescriptor().getFullMethodName(), new MethodCallHandler<>(def, handler));
    return this;
  }

  private static class MethodCallHandler<Req, Resp> implements Handler<GrpcRequest> {
    final ServerMethodDefinition<Req, Resp> def;
    final Handler<GrpcMethodCall<Req, Resp>> handler;
    MethodCallHandler(ServerMethodDefinition<Req, Resp> def, Handler<GrpcMethodCall<Req, Resp>> handler) {
      this.def = def;
      this.handler = handler;
    }
    @Override
    public void handle(GrpcRequest request) {
      GrpcMethodCall<Req, Resp> call = new GrpcMethodCall<>(request, def);
      request.messageHandler(msg -> {
        ByteArrayInputStream in = new ByteArrayInputStream(msg.data().getBytes());
        Req obj = def.getMethodDescriptor().parseRequest(in);
        Handler<Req> handler = call.handler;
        if (handler != null) {
          handler.handle(obj);
        }
      });
      request.endHandler(v -> {
        Handler<Void> handler = call.endHandler;
        if (handler != null) {
          handler.handle(null);
        }
      });
      handler.handle(call);
    }
  }
}
