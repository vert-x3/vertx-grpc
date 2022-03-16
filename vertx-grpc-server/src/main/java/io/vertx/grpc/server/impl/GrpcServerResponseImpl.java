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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServerResponse;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcServerResponseImpl<Req, Resp> implements GrpcServerResponse<Req, Resp> {

  private final HttpServerResponse httpResponse;
  private final Function<Resp, GrpcMessage> encoder;
  private String encoding = "identity";
  private GrpcStatus status = GrpcStatus.OK;
  private boolean headerSent;

  public GrpcServerResponseImpl(HttpServerResponse httpResponse, Function<Resp, GrpcMessage> encoder) {
    this.httpResponse = httpResponse;
    this.encoder = encoder;
  }

  public GrpcServerResponse<Req, Resp> status(GrpcStatus status) {
    Objects.requireNonNull(status);
    this.status = status;
    return this;
  }

  public GrpcServerResponse<Req, Resp> encoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  @Override
  public GrpcServerResponseImpl<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
    httpResponse.exceptionHandler(handler);
    return this;
  }

  @Override
  public Future<Void> write(Resp data) {
    return write(encoder.apply(data), false);
  }

  @Override
  public void write(Resp data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  public Future<Void> end() {
    return write(null, true);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override
  public GrpcServerResponse<Req, Resp> setWriteQueueMaxSize(int maxSize) {
    httpResponse.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return httpResponse.writeQueueFull();
  }

  @Override
  public GrpcServerResponse<Req, Resp> drainHandler(Handler<Void> handler) {
    httpResponse.drainHandler(handler);
    return this;
  }

  private Future<Void>  write(GrpcMessage message, boolean end) {
    MultiMap responseHeaders = httpResponse.headers();
    if (!headerSent) {
      headerSent = true;
      responseHeaders.set("content-type", "application/grpc");
      responseHeaders.set("grpc-encoding", encoding);
      responseHeaders.set("grpc-accept-encoding", "gzip");
      if (end) {
        responseHeaders.set("grpc-status", status.toString());
      }
    }
    if (end) {
      if (!responseHeaders.contains("grpc-status")) {
        MultiMap responseTrailers = httpResponse.trailers();
        responseTrailers.set("grpc-status", status.toString());
      }
      if (message != null) {
        return httpResponse.end(message.encode(encoding));
      } else {
        return httpResponse.end();
      }
    } else {
      return httpResponse.write(message.encode(encoding));
    }
  }
}
