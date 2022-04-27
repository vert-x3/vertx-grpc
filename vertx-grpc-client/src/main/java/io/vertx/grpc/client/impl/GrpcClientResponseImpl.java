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

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;

import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.impl.GrpcReadStreamBase;
import io.vertx.grpc.common.GrpcStatus;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientResponseImpl<Req, Resp> extends GrpcReadStreamBase<GrpcClientResponseImpl<Req, Resp>, Resp> implements GrpcClientResponse<Req, Resp> {

  private final GrpcClientRequestImpl<Req, Resp> request;
  private final HttpClientResponse httpResponse;
  private GrpcMessageDecoder<Resp> messageDecoder;
  private GrpcStatus status;
  private String encoding;

  public GrpcClientResponseImpl(GrpcClientRequestImpl<Req, Resp> request, HttpClientResponse httpResponse, GrpcMessageDecoder<Resp> messageDecoder) {
    super(Vertx.currentContext(), httpResponse, httpResponse.headers().get("grpc-encoding")); // A bit ugly
    this.request = request;
    this.encoding = httpResponse.headers().get("grpc-encoding");
    this.httpResponse = httpResponse;
    this.messageDecoder = messageDecoder;

    String responseStatus = httpResponse.getHeader("grpc-status");
    if (responseStatus != null) {
      status = GrpcStatus.valueOf(Integer.parseInt(responseStatus));
    }
  }

  @Override
  public MultiMap headers() {
    return httpResponse.headers();
  }

  @Override
  public String encoding() {
    return encoding;
  }

  @Override
  public MultiMap trailers() {
    return httpResponse.trailers();
  }

  protected void handleEnd() {
    String responseStatus = httpResponse.getTrailer("grpc-status");
    if (responseStatus != null) {
      status = GrpcStatus.valueOf(Integer.parseInt(responseStatus));
    }
    if (!request.trailersSent) {
      request.cancel();
    }
    super.handleEnd();
  }

  @Override
  public GrpcStatus status() {
    return status;
  }

  @Override
  public Future<Resp> last() {
    if (status == GrpcStatus.OK) {
      return super.last();
    } else {
      return context.failedFuture("");
    }
  }

  @Override
  public GrpcClientResponseImpl<Req, Resp> handler(Handler<Resp> handler) {
    if (handler != null) {
      return messageHandler(msg -> {
        GrpcMessage abc;
        switch (msg.encoding()) {
          case "identity":
            abc = msg;
            break;
          case "gzip": {
            try {
              abc = GrpcMessage.message("identity", GrpcMessageDecoder.GZIP.decode(msg));
            } catch (CodecException e) {
              request.cancel();
              return ;
            }
            break;
          }
          default:
            throw new UnsupportedOperationException();
        }
        Resp decoded = messageDecoder.decode(abc);
        handler.handle(decoded);
      });
    } else {
      return messageHandler(null);
    }
  }
}
