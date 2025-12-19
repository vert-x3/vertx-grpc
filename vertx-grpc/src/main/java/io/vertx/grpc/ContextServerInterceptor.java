/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An abstract interceptor that allows capturing data from the metadata to the vertx context so it can be used later
 * on the asynchronous APIs.
 *
 * @deprecated instead use Vert.x gRPC
 */
@Deprecated
public abstract class ContextServerInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextServerInterceptor.class);

  /**
   * This method is called before the {@link #interceptCall(ServerCall, Metadata, ServerCallHandler)} call happens and
   * allows extracting data from the metadata to the vert.x context.
   *
   * @param metadata the grpc connection context
   */
  public abstract void bind(Metadata metadata);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> handler) {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    if (ctx == null) {
      LOGGER.warn("Attempt to set contextual data from a non Vert.x thread");
    } else {
      bind(metadata);
    }

    return handler.startCall(call, metadata);
  }

  public static <T> T get(String key) {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    if (ctx != null) {
      ConcurrentMap<String, T> contextDataMap = contextualDataMap(ctx);
      return contextDataMap.get(key);
    }
    return null;
  }

  public static <T> T getOrDefault(String key, T defaultValue) {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    if (ctx != null) {
      ConcurrentMap<String, T> contextDataMap = contextualDataMap(ctx);
      return contextDataMap.getOrDefault(key, defaultValue);
    }
    return defaultValue;
  }

  @SuppressWarnings("unchecked")
  private static <T> ConcurrentMap<String, T> contextualDataMap(ContextInternal ctx) {
    Objects.requireNonNull(ctx);
    return (ConcurrentMap<String, T>)
      ctx
        .localContextData()
        .computeIfAbsent(ContextServerInterceptor.class, k -> new ConcurrentHashMap<>());
  }

  @SuppressWarnings("unchecked")
  public static <R,T> R put(String key, T value) {
    ContextInternal ctx = (ContextInternal) Vertx.currentContext();
    if (ctx != null) {
      return (R) contextualDataMap((ContextInternal) Vertx.currentContext())
        .put(key, value);
    } else {
      throw new IllegalStateException("No Vert.x Context found");
    }
  }
}
