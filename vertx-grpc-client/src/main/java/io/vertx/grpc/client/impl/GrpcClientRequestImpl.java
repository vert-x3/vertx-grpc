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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;

import java.util.Objects;
import java.util.function.Function;

import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.ServiceName;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientRequestImpl<Req, Resp> implements GrpcClientRequest<Req, Resp> {

  private final HttpClientRequest httpRequest;
  private final Function<Req, GrpcMessage> messageEncoder;
  private ServiceName serviceName;
  private String methodName;
  private String encoding = "identity";
  private boolean headerSent;
  private Future<GrpcClientResponse<Req, Resp>> response;

  public GrpcClientRequestImpl(HttpClientRequest httpRequest, Function<Req, GrpcMessage> messageEncoder, Function<GrpcMessage, Resp> messageDecoder) {
    this.httpRequest = httpRequest;
    this.messageEncoder = messageEncoder;
    this.response = httpRequest.response().map(httpResponse -> {
      GrpcClientResponseImpl<Req, Resp> grpcResponse = new GrpcClientResponseImpl<>(httpResponse, messageDecoder);
      grpcResponse.init();
      return grpcResponse;
    });
  }

  @Override
  public GrpcClientRequest<Req, Resp> serviceName(ServiceName serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  @Override
  public GrpcClientRequest<Req, Resp> fullMethodName(String fullMethodName) {
    if (headerSent) {
      throw new IllegalStateException("Request already sent");
    }
    int idx = fullMethodName.lastIndexOf('/');
    if (idx == -1) {
      throw new IllegalArgumentException();
    }
    this.serviceName = ServiceName.create(fullMethodName.substring(0, idx));
    this.methodName = fullMethodName.substring(idx + 1);
    return this;
  }

  @Override
  public GrpcClientRequest<Req, Resp> methodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

  @Override public GrpcClientRequest<Req, Resp> encoding(String encoding) {
    Objects.requireNonNull(encoding);
    this.encoding = encoding;
    return this;
  }

  @Override
  public GrpcClientRequest<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    httpRequest.exceptionHandler(handler);
    return this;
  }

  @Override
  public void write(Req data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override
  public GrpcClientRequest<Req, Resp> setWriteQueueMaxSize(int maxSize) {
    httpRequest.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return httpRequest.writeQueueFull();
  }

  @Override
  public GrpcClientRequest<Req, Resp> drainHandler(Handler<Void> handler) {
    httpRequest.drainHandler(handler);
    return this;
  }

  @Override public Future<Void> write(Req message) {
    return write(messageEncoder.apply(message), false);
  }

  @Override public Future<Void> end(Req message) {
    return write(messageEncoder.apply(message), true);
  }

  @Override public Future<Void> end() {
    if (!headerSent) {
      throw new IllegalStateException();
    }
    return httpRequest.end();
  }

  private Future<Void> write(GrpcMessage message, boolean end) {
    if (!headerSent) {
      ServiceName serviceName = this.serviceName;
      String methodName = this.methodName;
      if (serviceName == null) {
        throw new IllegalStateException();
      }
      if (methodName == null) {
        throw new IllegalStateException();
      }
      String uri = serviceName.pathOf(methodName);
      httpRequest.putHeader("content-type", "application/grpc");
      httpRequest.putHeader("grpc-encoding", encoding);
      httpRequest.putHeader("grpc-accept-encoding", "gzip");
      httpRequest.putHeader("te", "trailers");
      httpRequest.setChunked(true);
      httpRequest.setURI(uri);
      headerSent = true;
    }
    if (end) {
      return httpRequest.end(message.encode(encoding));
    } else {
      return httpRequest.write(message.encode(encoding));
    }
  }

  @Override public Future<GrpcClientResponse<Req, Resp>> response() {
    return response;
  }

  @Override
  public void reset() {
    httpRequest.reset();
  }
}
