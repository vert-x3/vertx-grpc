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
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

import java.util.Map;
import java.util.Objects;

import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.MessageDecoder;
import io.vertx.grpc.common.MessageEncoder;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.impl.BaseGrpcMessage;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientRequestImpl<Req, Resp> implements GrpcClientRequest<Req, Resp> {

  private final HttpClientRequest httpRequest;
  private final MessageEncoder<Req> messageEncoder;
  private ServiceName serviceName;
  private String methodName;
  private String encoding = null;
  private boolean headersSent;
  private Future<GrpcClientResponse<Req, Resp>> response;
  private MultiMap headers;

  public GrpcClientRequestImpl(HttpClientRequest httpRequest, MessageEncoder<Req> messageEncoder, MessageDecoder<Resp> messageDecoder) {
    this.httpRequest = httpRequest;
    this.messageEncoder = messageEncoder;
    this.response = httpRequest.response().map(httpResponse -> {
      GrpcClientResponseImpl<Req, Resp> grpcResponse = new GrpcClientResponseImpl<>(httpResponse, messageDecoder);
      grpcResponse.init();
      return grpcResponse;
    });
  }

  @Override
  public MultiMap headers() {
    if (headersSent) {
      throw new IllegalStateException("Headers already sent");
    }
    if (headers == null) {
      headers = MultiMap.caseInsensitiveMultiMap();
    }
    return headers;
  }

  @Override
  public GrpcClientRequest<Req, Resp> serviceName(ServiceName serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  @Override
  public GrpcClientRequest<Req, Resp> fullMethodName(String fullMethodName) {
    if (headersSent) {
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

  @Override public Future<Void> writeMessage(GrpcMessage message) {
    return writeMessage(message, false);
  }

  @Override public Future<Void> endMessage(GrpcMessage message) {
    return writeMessage(message, true);
  }

  @Override public Future<Void> end() {
    if (!headersSent) {
      throw new IllegalStateException();
    }
    return httpRequest.end();
  }

  private Future<Void> writeMessage(GrpcMessage message, boolean end) {
    if (!headersSent) {
      ServiceName serviceName = this.serviceName;
      String methodName = this.methodName;
      if (serviceName == null) {
        throw new IllegalStateException();
      }
      if (methodName == null) {
        throw new IllegalStateException();
      }
      if (headers != null) {
        MultiMap requestHeaders = httpRequest.headers();
        for (Map.Entry<String, String> header : headers) {
          if (!header.getKey().startsWith("grpc-")) {
            requestHeaders.add(header.getKey(), header.getValue());
          } else {
            // Log ?
          }
        }
      }
      String uri = serviceName.pathOf(methodName);
      httpRequest.putHeader("content-type", "application/grpc");
      if (encoding != null) {
        httpRequest.putHeader("grpc-encoding", encoding);
      }
      httpRequest.putHeader("grpc-accept-encoding", "gzip");
      httpRequest.putHeader("te", "trailers");
      httpRequest.setChunked(true);
      httpRequest.setURI(uri);
      headersSent = true;
    }

    if (encoding != null && !encoding.equals(message.encoding())) {
      switch (encoding) {
        case "gzip":
          message = MessageEncoder.GZIP.encode(message.payload());
          break;
        case "identity":
          if (!message.encoding().equals("identity")) {
            if (!message.encoding().equals("gzip")) {
              throw new UnsupportedOperationException("Not implemented");
            }
            Buffer decoded = MessageDecoder.GZIP.decode(message);
            message = new GrpcMessage() {
              @Override
              public String encoding() {
                return "identity";
              }
              @Override
              public Buffer payload() {
                return decoded;
              }
            };
          }
          break;
      }
    }

    if (end) {
      return httpRequest.end(BaseGrpcMessage.encode(message));
    } else {
      return httpRequest.write(BaseGrpcMessage.encode(message));
    }
  }

  @Override
  public Future<Void> write(Req message) {
    return writeMessage(messageEncoder.encode(message));
  }

  @Override
  public Future<Void> end(Req message) {
    return endMessage(messageEncoder.encode(message));
  }

  @Override
  public void write(Req data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    end().onComplete(handler);
  }

  @Override public Future<GrpcClientResponse<Req, Resp>> response() {
    return response;
  }

  @Override
  public void reset() {
    httpRequest.reset();
  }
}
