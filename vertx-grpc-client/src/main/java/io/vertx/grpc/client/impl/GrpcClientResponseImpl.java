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
package io.vertx.grpc.client.impl;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;

import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.impl.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcStatus;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientResponseImpl<Req, Resp> extends GrpcMessageDecoder implements GrpcClientResponse<Req, Resp> {

  private final HttpClientResponse httpResponse;
  private final Function<GrpcMessage, Resp> messageDecoder;
  private GrpcStatus status;
  private Handler<Resp> messageHandler;
  private Handler<Void> endHandler;

  public GrpcClientResponseImpl(HttpClientResponse httpResponse, Function<GrpcMessage, Resp> messageDecoder) {
    super(Vertx.currentContext(), httpResponse, httpResponse.headers().get("grpc-encoding")); // A bit ugly
    this.httpResponse = httpResponse;
    this.messageDecoder = messageDecoder;
  }

  @Override
  protected void handleMessage(GrpcMessage msg) {
    Handler<Resp> handler = messageHandler;
    if (handler != null) {
      handler.handle(messageDecoder.apply(msg));
    }
  }

  @Override
  public MultiMap headers() {
    return httpResponse.headers();
  }

  @Override
  public MultiMap trailers() {
    return httpResponse.trailers();
  }

  protected void handleEnd() {
    String responseStatus = httpResponse.getTrailer("grpc-status");
    if (responseStatus == null) {
      responseStatus = httpResponse.getHeader("grpc-status");
    }
    if (responseStatus != null) {
      status = GrpcStatus.valueOf(Integer.parseInt(responseStatus));
    }
    Handler<Void> handler = endHandler;
    if (handler != null) {
      handler.handle(null);
    }
  }

  @Override public GrpcStatus status() {
    return status;
  }

  @Override public GrpcClientResponse<Req, Resp> messageHandler(Handler<Resp> handler) {
    messageHandler = handler;
    return this;
  }

  @Override
  public GrpcClientResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    httpResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public GrpcClientResponse<Req, Resp> handler(Handler<Resp> handler) {
    return messageHandler(handler);
  }

  @Override public GrpcClientResponse<Req, Resp> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  @Override
  public GrpcClientResponseImpl<Req, Resp> pause() {
    return (GrpcClientResponseImpl) super.pause();
  }

  @Override
  public GrpcClientResponseImpl<Req, Resp> resume() {
    return (GrpcClientResponseImpl) super.resume();
  }

  @Override
  public GrpcClientResponseImpl<Req, Resp> fetch(long amount) {
    return (GrpcClientResponseImpl) super.fetch(amount);
  }
}
