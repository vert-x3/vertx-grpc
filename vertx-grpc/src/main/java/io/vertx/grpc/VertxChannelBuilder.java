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

import io.grpc.*;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.DelegatingSslContext;
import io.netty.handler.ssl.SslContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.impl.SSLHelper;
import io.vertx.core.net.impl.transport.Transport;

import javax.annotation.Nullable;
import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxChannelBuilder extends ManagedChannelBuilder<VertxChannelBuilder> {

  public static VertxChannelBuilder forTarget(Vertx vertx, String target) {
    return new VertxChannelBuilder(vertx, target);
  }

  public static VertxChannelBuilder forAddress(Vertx vertx, String host, int port) {
    return new VertxChannelBuilder(vertx, host, port);
  }

  public static VertxChannelBuilder forAddress(Vertx vertx, SocketAddress address) {
    return new VertxChannelBuilder(vertx, address);
  }

  private final Vertx vertx;
  private final NettyChannelBuilder builder;
  private final ContextInternal context;
  private final HttpClientOptions options = new HttpClientOptions();

  private VertxChannelBuilder(Vertx vertx, String host, int port) {
    this(vertx, GrpcUtil.authorityFromHostAndPort(host, port));
  }

  private VertxChannelBuilder(Vertx vertx, String target) {
    this.vertx = vertx;
    this.builder = NettyChannelBuilder.forTarget(target);
    this.context = (ContextInternal) vertx.getOrCreateContext();
  }

  private VertxChannelBuilder(Vertx vertx, SocketAddress address) {
    this.vertx = vertx;
    this.builder = NettyChannelBuilder.forAddress(address);
    this.context = (ContextInternal) vertx.getOrCreateContext();
  }

  /**
   * @return the underlying {@code NettyChannelBuilder}
   */
  public NettyChannelBuilder nettyBuilder() {
    return builder;
  }

  @Override
  public VertxChannelBuilder directExecutor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VertxChannelBuilder executor(Executor executor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VertxChannelBuilder intercept(List<ClientInterceptor> interceptors) {
    builder.intercept(interceptors);
    return this;
  }

  @Override
  public VertxChannelBuilder intercept(ClientInterceptor... interceptors) {
    builder.intercept(interceptors);
    return this;
  }

  @Override
  public VertxChannelBuilder userAgent(String userAgent) {
    builder.userAgent(userAgent);
    return this;
  }

  @Override
  public VertxChannelBuilder overrideAuthority(String authority) {
    builder.overrideAuthority(authority);
    return this;
  }

  @Override
  @Deprecated
  public VertxChannelBuilder nameResolverFactory(NameResolver.Factory resolverFactory) {
    builder.nameResolverFactory(resolverFactory);
    return this;
  }

  @Override
  public VertxChannelBuilder offloadExecutor(Executor executor) {
    builder.offloadExecutor(executor);
    return this;
  }

  @Override
  public VertxChannelBuilder defaultLoadBalancingPolicy(String policy) {
    builder.defaultLoadBalancingPolicy(policy);
    return this;
  }

  @Override
  public VertxChannelBuilder maxInboundMetadataSize(int bytes) {
    builder.maxInboundMetadataSize(bytes);
    return this;
  }

  @Override
  public VertxChannelBuilder proxyDetector(ProxyDetector proxyDetector) {
    builder.proxyDetector(proxyDetector);
    return this;
  }

  @Override
  public VertxChannelBuilder defaultServiceConfig(@Nullable Map<String, ?> serviceConfig) {
    builder.defaultServiceConfig(serviceConfig);
    return this;
  }

  @Override
  public VertxChannelBuilder disableServiceConfigLookUp() {
    builder.disableServiceConfigLookUp();
    return this;
  }

  @Override
  public VertxChannelBuilder decompressorRegistry(DecompressorRegistry registry) {
    builder.decompressorRegistry(registry);
    return this;
  }

  @Override
  public VertxChannelBuilder compressorRegistry(CompressorRegistry registry) {
    builder.compressorRegistry(registry);
    return this;
  }

  @Override
  public VertxChannelBuilder idleTimeout(long value, TimeUnit unit) {
    builder.idleTimeout(value, unit);
    return this;
  }

  @Override
  public VertxChannelBuilder maxInboundMessageSize(int max) {
    builder.maxInboundMessageSize(max);
    return this;
  }

  @Override
  public VertxChannelBuilder usePlaintext() {
    builder.usePlaintext();
    return this;
  }

  @Override
  public VertxChannelBuilder useTransportSecurity() {
    builder.useTransportSecurity();
    return this;
  }

  @Override
  public VertxChannelBuilder enableFullStreamDecompression() {
    builder.enableFullStreamDecompression();
    return this;
  }

  @Override
  public VertxChannelBuilder keepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
    builder.keepAliveTime(keepAliveTime, timeUnit);
    return this;
  }

  @Override
  public VertxChannelBuilder keepAliveTimeout(long keepAliveTimeout, TimeUnit timeUnit) {
    builder.keepAliveTimeout(keepAliveTimeout, timeUnit);
    return this;
  }

  @Override
  public VertxChannelBuilder keepAliveWithoutCalls(boolean enable) {
    builder.keepAliveWithoutCalls(enable);
    return this;
  }

  @Override
  public VertxChannelBuilder maxRetryAttempts(int maxRetryAttempts) {
    builder.maxRetryAttempts(maxRetryAttempts);
    return this;
  }

  @Override
  public VertxChannelBuilder maxHedgedAttempts(int maxHedgedAttempts) {
    builder.maxHedgedAttempts(maxHedgedAttempts);
    return this;
  }

  @Override
  public VertxChannelBuilder retryBufferSize(long bytes) {
    builder.retryBufferSize(bytes);
    return this;
  }

  @Override
  public VertxChannelBuilder perRpcBufferLimit(long bytes) {
    builder.perRpcBufferLimit(bytes);
    return this;
  }

  @Override
  public VertxChannelBuilder disableRetry() {
    builder.disableRetry();
    return this;
  }

  @Override
  public VertxChannelBuilder enableRetry() {
    builder.enableRetry();
    return this;
  }

  @Override
  public VertxChannelBuilder setBinaryLog(BinaryLog binaryLog) {
    builder.setBinaryLog(binaryLog);
    return this;
  }

  @Override
  public VertxChannelBuilder maxTraceEvents(int maxTraceEvents) {
    builder.maxTraceEvents(maxTraceEvents);
    return this;
  }

  public VertxChannelBuilder useSsl(Handler<ClientOptionsBase> handler) {
    handler.handle(options);
    return this;
  }

  @Override
  public ManagedChannel build() {
    // SSL
    if (options.isSsl()) {
      SSLHelper helper = new SSLHelper(options, options.getKeyCertOptions(), options.getTrustOptions());
      helper.setApplicationProtocols(Collections.singletonList(HttpVersion.HTTP_2));
      SslContext ctx = helper.getContext((VertxInternal) vertx);
      builder.sslContext(new DelegatingSslContext(ctx) {
        @Override
        protected void initEngine(SSLEngine engine) {
          helper.configureEngine(engine, null);
        }
      });
    }
    Transport transport = ((VertxInternal) vertx).transport();
    return builder
      .eventLoopGroup(context.nettyEventLoop())
      .channelFactory(transport.channelFactory(false))
      .executor(command -> {
      if (Context.isOnEventLoopThread()) {
        context.dispatch(event -> command.run());
      } else {
        command.run();
      }
    }).build();
  }
}
