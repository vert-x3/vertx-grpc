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

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.MessageDecoder;
import io.vertx.grpc.common.MessageEncoder;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.impl.GrpcMessageDecoder;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.grpc.server.GrpcServerResponse;

import static io.vertx.grpc.common.GrpcError.mapHttp2ErrorCode;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcServerRequestImpl<Req, Resp> extends GrpcMessageDecoder implements GrpcServerRequest<Req, Resp> {

  final HttpServerRequest httpRequest;
  final GrpcServerResponse<Req, Resp> response;
  private Handler<GrpcMessage> messageHandler;
  private Handler<GrpcError> errorHandler;
  private Handler<Throwable> exceptionHandler;
  private MessageDecoder<Req> messageDecoder;
  private Handler<Void> endHandler;
  private GrpcMethodCall methodCall;

  public GrpcServerRequestImpl(HttpServerRequest httpRequest, MessageDecoder<Req> messageDecoder, MessageEncoder<Resp> messageEncoder, GrpcMethodCall methodCall) {
    super(((HttpServerRequestInternal) httpRequest).context(), httpRequest, httpRequest.headers().get("grpc-encoding"));
    this.httpRequest = httpRequest;
    this.response = new GrpcServerResponseImpl<>(httpRequest.response(), messageEncoder);
    this.methodCall = methodCall;
    this.messageDecoder = messageDecoder;
  }

  public String fullMethodName() {
    return methodCall.fullMethodName();
  }

  @Override
  public MultiMap headers() {
    return httpRequest.headers();
  }

  @Override
  public String encoding() {
    return httpRequest.getHeader("grpc-encoding");
  }

  @Override
  public ServiceName serviceName() {
    return methodCall.serviceName();
  }

  @Override
  public String methodName() {
    return methodCall.methodName();
  }

  protected void handleMessage(GrpcMessage message) {
    Handler<GrpcMessage> msgHandler = messageHandler;
    if (msgHandler != null) {
      msgHandler.handle(message);
    }
  }

  protected void handleEnd() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  @Override
  protected void handleReset(long code) {
    Handler<GrpcError> handler = errorHandler;
    if (handler != null) {
      GrpcError error = mapHttp2ErrorCode(code);
      if (error != null) {
        handler.handle(error);
      }
    }
  }

  @Override
  protected void handleException(Throwable err) {
    Handler<Throwable> handler = exceptionHandler;
    if (handler != null) {
      handler.handle(err);
    }
  }

  @Override
  public GrpcServerRequest<Req, Resp> handler(Handler<Req> handler) {
    if (handler != null) {
      return messageHandler(msg -> {
        GrpcMessage abc;
        switch (msg.encoding()) {
          case "identity":
            abc = msg;
            break;
          case "gzip": {
            try {
              abc = GrpcMessage.message("identity", MessageDecoder.GZIP.decode(msg));
            } catch (CodecException e) {
              response.cancel();
              return;
            }
            break;
          }
          default:
            throw new UnsupportedOperationException();
        }
        Req decoded = messageDecoder.decode(abc);
        handler.handle(decoded);
      });
    } else {
      return messageHandler(null);
    }
  }

  @Override
  public GrpcServerRequest<Req, Resp> errorHandler(Handler<GrpcError> handler) {
    this.errorHandler = handler;
    return this;
  }

  public GrpcServerRequest<Req, Resp> messageHandler(Handler<GrpcMessage> handler) {
    this.messageHandler = handler;
    return this;
  }

  public GrpcServerRequest<Req, Resp> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  @Override
  public GrpcServerRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  public GrpcServerResponse<Req, Resp> response() {
    return response;
  }

  @Override
  public GrpcServerRequestImpl pause() {
    return (GrpcServerRequestImpl) super.pause();
  }

  @Override
  public GrpcServerRequestImpl resume() {
    return (GrpcServerRequestImpl) super.resume();
  }

  @Override
  public GrpcServerRequestImpl fetch(long amount) {
    return (GrpcServerRequestImpl) super.fetch(amount);
  }
}
