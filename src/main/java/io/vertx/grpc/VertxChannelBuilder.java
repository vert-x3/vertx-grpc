package io.vertx.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyChannelBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

import java.net.SocketAddress;
import java.util.List;
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
  private NettyChannelBuilder builder;
  private ContextInternal context;

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
  public VertxChannelBuilder usePlaintext(boolean skipNegotiation) {
    builder.usePlaintext(skipNegotiation);
    return this;
  }

  @Override
  public VertxChannelBuilder nameResolverFactory(NameResolver.Factory resolverFactory) {
    builder.nameResolverFactory(resolverFactory);
    return this;
  }

  @Override
  public VertxChannelBuilder loadBalancerFactory(LoadBalancer.Factory loadBalancerFactory) {
    builder.loadBalancerFactory(loadBalancerFactory);
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
  public ManagedChannel build() {
    return builder.eventLoopGroup(context.nettyEventLoop()).executor(command -> {
      if (Context.isOnEventLoopThread()) {
        context.executeFromIO(command::run);
      } else {
        command.run();
      }
    }).build();
  }
}
