package io.vertx.grpc;

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

import javax.annotation.Nullable;
import java.io.File;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxServerBuilder extends ServerBuilder<VertxServerBuilder> {

  public static VertxServerBuilder forPort(Vertx vertx, int port) {
    return new VertxServerBuilder(vertx, port);
  }

  public static VertxServerBuilder forAddress(Vertx vertx, SocketAddress address) {
    return new VertxServerBuilder(vertx, address);
  }

  private Vertx vertx;
  private NettyServerBuilder builder;
  private ContextInternal context;

  private VertxServerBuilder(Vertx vertx, int port) {
    this.vertx = vertx;
    this.context = (ContextInternal) vertx.getOrCreateContext();
    this.builder = NettyServerBuilder.forPort(port);
  }

  private VertxServerBuilder(Vertx vertx, SocketAddress address) {
    this.vertx = vertx;
    this.builder = NettyServerBuilder.forAddress(address);
  }

  public VertxServerBuilder directExecutor() {
    throw new UnsupportedOperationException();
  }

  public VertxServerBuilder executor(@Nullable Executor executor) {
    throw new UnsupportedOperationException();
  }

  public VertxServerBuilder addService(ServerServiceDefinition service) {
    builder.addService(service);
    return this;
  }

  public VertxServerBuilder addService(BindableService bindableService) {
    builder.addService(bindableService);
    return this;
  }

  public VertxServerBuilder fallbackHandlerRegistry(@Nullable HandlerRegistry fallbackRegistry) {
    builder.fallbackHandlerRegistry(fallbackRegistry);
    return this;
  }

  public VertxServerBuilder useTransportSecurity(File certChain, File privateKey) {
    builder.useTransportSecurity(certChain, privateKey);
    return this;
  }

  public VertxServerBuilder decompressorRegistry(@Nullable DecompressorRegistry registry) {
    builder.decompressorRegistry(registry);
    return this;
  }

  public VertxServerBuilder compressorRegistry(@Nullable CompressorRegistry registry) {
    builder.compressorRegistry(registry);
    return this;
  }

  public Server build() {
    return builder
        .executor(command -> context.executeFromIO(command::run))
        .workerEventLoopGroup(context.nettyEventLoop()).build();
  }
}
