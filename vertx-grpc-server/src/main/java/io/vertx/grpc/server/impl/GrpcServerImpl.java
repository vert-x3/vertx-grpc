/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.server.impl;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcServerImpl implements GrpcServer {

  private Handler<GrpcServerRequest<GrpcMessage, GrpcMessage>> requestHandler;
  private Map<String, MethodCallHandler<?, ?>> methodCallHandlers = new HashMap<>();

  @Override
  public void handle(HttpServerRequest httpRequest) {
    GrpcMethodCall methodCall = new GrpcMethodCall(httpRequest.path());
    String fmn = methodCall.fullMethodName();
    MethodCallHandler<?, ?> method = methodCallHandlers.get(fmn);
    if (method != null) {
      handle(method, httpRequest, methodCall);
    } else {
      Handler<GrpcServerRequest<GrpcMessage, GrpcMessage>> handler = requestHandler;
      if (handler != null) {
        GrpcServerRequestImpl<GrpcMessage, GrpcMessage> grpcRequest = new GrpcServerRequestImpl<>(httpRequest, Function.identity(), Function.identity(), methodCall);
        grpcRequest.init();
        handler.handle(grpcRequest);
      } else {
        httpRequest.response().setStatusCode(500).end();
      }
    }
  }

  private <Req, Resp> void handle(MethodCallHandler<Req, Resp> method, HttpServerRequest httpRequest, GrpcMethodCall methodCall) {
    Function<GrpcMessage, Req> decoder = msg -> {
      ByteArrayInputStream in = new ByteArrayInputStream(msg.payload().getBytes());
      return method.def.parseRequest(in);
    };
    Function<Resp, GrpcMessage> encoder = msg -> {
      Buffer encoded = Buffer.buffer();
      InputStream stream = method.def.streamResponse(msg);
      byte[] tmp = new byte[256];
      int i;
      try {
        while ((i = stream.read(tmp)) != -1) {
          encoded.appendBytes(tmp, 0, i);
        }
      } catch (IOException e) {
        throw new VertxException(e);
      }
      return GrpcMessage.message(encoded);
    };
    GrpcServerRequestImpl<Req, Resp> grpcRequest = new GrpcServerRequestImpl<>(httpRequest, decoder, encoder, methodCall);
    grpcRequest.init();
    method.handle(grpcRequest);
  }

  public GrpcServer callHandler(Handler<GrpcServerRequest<GrpcMessage, GrpcMessage>> handler) {
    this.requestHandler = handler;
    return this;
  }

  public <Req, Resp> GrpcServer callHandler(MethodDescriptor<Req, Resp> methodDesc, Handler<GrpcServerRequest<Req, Resp>> handler) {
    if (handler != null) {
      methodCallHandlers.put(methodDesc.getFullMethodName(), new MethodCallHandler<>(methodDesc, handler));
    } else {
      methodCallHandlers.remove(methodDesc.getFullMethodName());
    }
    return this;
  }

  private static class MethodCallHandler<Req, Resp> implements Handler<GrpcServerRequest<Req, Resp>> {

    final MethodDescriptor<Req, Resp> def;
    final Handler<GrpcServerRequest<Req, Resp>> handler;

    MethodCallHandler(MethodDescriptor<Req, Resp> def, Handler<GrpcServerRequest<Req, Resp>> handler) {
      this.def = def;
      this.handler = handler;
    }

    @Override
    public void handle(GrpcServerRequest<Req, Resp> grpcRequest) {
      handler.handle(grpcRequest);
    }
  }
}
