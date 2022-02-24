package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class GrpcServer implements Handler<HttpServerRequest> {

  private Handler<GrpcServerRequest> requestHandler;
  private Map<String, MethodCallHandler<?, ?>> methodCallHandlers = new HashMap<>();

  @Override
  public void handle(HttpServerRequest request) {
    GrpcServerRequest grpcRequest = new GrpcServerRequest(request);
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

  private void handleUnary(GrpcServerRequest request) {
    String fmn = request.fullMethodName();
    MethodCallHandler<?, ?> method = methodCallHandlers.get(fmn);
    if (method != null) {
      method.handle(request);
    } else {
      Handler<GrpcServerRequest> handler = requestHandler;
      if (handler != null) {
        handler.handle(request);
      } else {
        request.httpRequest.response().setStatusCode(500).end();
      }
    }
  }

  public GrpcServer requestHandler(Handler<GrpcServerRequest> requestHandler) {
    this.requestHandler = requestHandler;
    return this;
  }

  public <Req, Resp> GrpcServer methodCallHandler(MethodDescriptor<Req, Resp> methodDesc, Handler<GrpcServerMethodCall<Req, Resp>> handler) {
    methodCallHandlers.put(methodDesc.getFullMethodName(), new MethodCallHandler<>(methodDesc, handler));
    return this;
  }

  private static class MethodCallHandler<Req, Resp> implements Handler<GrpcServerRequest> {
    final MethodDescriptor<Req, Resp> def;
    final Handler<GrpcServerMethodCall<Req, Resp>> handler;
    MethodCallHandler(MethodDescriptor<Req, Resp> def, Handler<GrpcServerMethodCall<Req, Resp>> handler) {
      this.def = def;
      this.handler = handler;
    }
    @Override
    public void handle(GrpcServerRequest request) {
      GrpcServerMethodCall<Req, Resp> methodCall = new GrpcServerMethodCall<>(request, def);
      request.messageHandler(msg -> {
        ByteArrayInputStream in = new ByteArrayInputStream(msg.data().getBytes());
        Req obj = def.parseRequest(in);
        Handler<Req> handler = methodCall.handler;
        if (handler != null) {
          handler.handle(obj);
        }
      });
      request.endHandler(v -> {
        Handler<Void> handler = methodCall.endHandler;
        if (handler != null) {
          handler.handle(null);
        }
      });
      handler.handle(methodCall);
    }
  }
}
