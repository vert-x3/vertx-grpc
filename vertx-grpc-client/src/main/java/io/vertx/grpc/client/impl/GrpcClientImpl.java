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

import io.grpc.MethodDescriptor;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.client.GrpcClientStreamingCall;
import io.vertx.grpc.client.GrpcServerStreamingCall;
import io.vertx.grpc.client.GrpcUnaryCall;
import io.vertx.grpc.common.GrpcMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GrpcClientImpl implements GrpcClient {

  private final Vertx vertx;
  private HttpClient client;

  public GrpcClientImpl(HttpClientOptions options, Vertx vertx) {
    this.vertx = vertx;
    this.client = vertx.createHttpClient(new HttpClientOptions(options)
      .setProtocolVersion(HttpVersion.HTTP_2));
  }

  public GrpcClientImpl(Vertx vertx) {
    this(new HttpClientOptions().setHttp2ClearTextUpgrade(false), vertx);
  }

  @Override public Future<GrpcClientRequest<GrpcMessage, GrpcMessage>> request(SocketAddress server) {
    RequestOptions options = new RequestOptions()
      .setMethod(HttpMethod.POST)
      .setServer(server);
    return client.request(options)
      .map(request -> new GrpcClientRequestImpl<>(request, Function.identity(), Function.identity()));
  }

  @Override public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(SocketAddress server, MethodDescriptor<Req, Resp> method) {
    Function<Req, GrpcMessage> encoder = msg -> {
      Buffer encoded = Buffer.buffer();
      InputStream stream = method.streamRequest(msg);
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
    Function<GrpcMessage, Resp> decoder = msg -> {
      ByteArrayInputStream in = new ByteArrayInputStream(msg.payload().getBytes());
      return method.parseResponse(in);
    };
    RequestOptions options = new RequestOptions()
      .setMethod(HttpMethod.POST)
      .setServer(server);
    return client.request(options)
      .map(request -> {
        GrpcClientRequestImpl<Req, Resp> call = new GrpcClientRequestImpl<>(request, encoder, decoder);
        call.fullMethodName(method.getFullMethodName());
        return call;
      });
  }

  @Override
  public <Req, Resp> GrpcUnaryCall<Req, Resp> unaryCall(MethodDescriptor<Req, Resp> method) {
    if (method.getType() != MethodDescriptor.MethodType.UNARY) {
      throw new IllegalArgumentException();
    }
    return (server, msg) -> {
      ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
      return request(server, method).compose(r -> {
        r.end(msg);
        return r.response().compose(resp -> {
          Promise<Resp> p = ctx.promise();
          resp.exceptionHandler(err -> {
            p.tryFail(err);
          });
          resp.handler(m -> {
            p.tryComplete(m);
          });
          return p.future();
        });
      });
    };
  }

  @Override
  public <Req, Resp> GrpcClientStreamingCall<Req, Resp> clientStreamingCall(MethodDescriptor<Req, Resp> method) {
    if (method.getType() != MethodDescriptor.MethodType.CLIENT_STREAMING) {
      throw new IllegalArgumentException();
    }
    return new GrpcClientStreamingCall<Req, Resp>() {
      @Override
      public Future<Resp> call(SocketAddress server, ReadStream<Req> stream) {
        ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
        Pipe<Req> pipe = stream.pipe();
        pipe.endOnFailure(false);
        pipe.endOnSuccess(true);
        return request(server, method).transform(ar -> {
          if (ar.succeeded()) {
            GrpcClientRequest<Req, Resp> a = ar.result();
            Future<Void> res = pipe.to(a);
            res.onFailure(err -> {
              a.reset();
            });
            return a.response().compose(resp -> {
              Promise<Resp> p = ctx.promise();
              resp.handler(m -> {
                p.tryComplete(m);
              });
              return p.future();
            });
          } else {
            return ctx.failedFuture(ar.cause());
          }
        });
      }
    };
  }

  @Override
  public <Req, Resp> GrpcServerStreamingCall<Req, Resp> serverStreamingCall(MethodDescriptor<Req, Resp> method) {
    if (method.getType() != MethodDescriptor.MethodType.SERVER_STREAMING) {
      throw new IllegalArgumentException();
    }
    return (server, msg) -> request(server, method).compose(r -> {
      r.end(msg);
      Future<ReadStream<Resp>> response = (Future) r.response();
      return response;
    });
  }
}
