package io.vertx.grpc;

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.HandlerRegistry;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerTransportFilter;
import io.grpc.netty.NettyServerBuilder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.net.TCPSSLOptions;
import io.vertx.core.net.impl.ServerID;

import javax.annotation.Nullable;
import java.io.File;
import java.net.InetSocketAddress;
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

  public static VertxServerBuilder forAddress(Vertx vertx, String host, int port) {
    return new VertxServerBuilder(vertx, new InetSocketAddress(host, port));
  }

  private final ServerID id;
  private Vertx vertx;
  private NettyServerBuilder builder;
  private HttpServerOptions options = new HttpServerOptions();

  private VertxServerBuilder(Vertx vertx, int port) {
    this.id = new ServerID(port, "0.0.0.0");
    this.vertx = vertx;
    this.builder = NettyServerBuilder.forPort(port);
  }

  private VertxServerBuilder(Vertx vertx, SocketAddress address) {
    this.id = new ServerID(((InetSocketAddress) address).getPort(), ((InetSocketAddress) address).getHostString());
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

  @Override
  public VertxServerBuilder addTransportFilter(ServerTransportFilter filter) {
    builder.addTransportFilter(filter);
    return this;
  }

  @Override
  public VertxServerBuilder addStreamTracerFactory(ServerStreamTracer.Factory factory) {
    builder.addStreamTracerFactory(factory);
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

  public VertxServerBuilder useSsl(Handler<TCPSSLOptions> handler) {
    handler.handle(options);
    return this;
  }

  public VertxServer build() {
    ContextImpl context = (ContextImpl) vertx.getOrCreateContext();
    return new VertxServer(id, options, builder, context);
  }
}
