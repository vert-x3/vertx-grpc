package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class GrpcServer implements Handler<HttpServerRequest> {

  private Handler<GrpcServerRequest> requestHandler;
  private Map<String, MethodCallHandler<?, ?>> methodCallHandlers = new HashMap<>();

  @Override
  public void handle(HttpServerRequest httpRequest) {
    GrpcServerRequest grpcRequest = new GrpcServerRequest(httpRequest);
    grpcRequest.init();
    handle(grpcRequest);
  }

  private void handle(GrpcServerRequest request) {
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

  public <Req, Resp> GrpcServer callHandler(MethodDescriptor<Req, Resp> methodDesc, Handler<GrpcServerCallRequest<Req, Resp>> handler) {
    methodCallHandlers.put(methodDesc.getFullMethodName(), new MethodCallHandler<>(methodDesc, handler));
    return this;
  }

  private static class MethodCallHandler<Req, Resp> implements Handler<GrpcServerRequest> {

    final MethodDescriptor<Req, Resp> def;
    final Handler<GrpcServerCallRequest<Req, Resp>> handler;

    MethodCallHandler(MethodDescriptor<Req, Resp> def, Handler<GrpcServerCallRequest<Req, Resp>> handler) {
      this.def = def;
      this.handler = handler;
    }

    @Override
    public void handle(GrpcServerRequest request) {
      GrpcServerCallRequest<Req, Resp> methodCall = new GrpcServerCallRequest<>(request, def);
      request.messageHandler(msg -> {
        ByteArrayInputStream in = new ByteArrayInputStream(msg.data().getBytes());
        Req requestMessage = def.parseRequest(in);
        methodCall.handleMessage(requestMessage);
      });
      request.endHandler(v -> {
        methodCall.handleEnd();
      });
      handler.handle(methodCall);
    }
  }
}
