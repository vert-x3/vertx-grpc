package io.vertx.grpc;

import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.impl.ServerID;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
  private VertxInternal vertx;
  private NettyServerBuilder builder;
  private HttpServerOptions options = new HttpServerOptions();
  private Consumer<Runnable> commandDecorator;

  private VertxServerBuilder(Vertx vertx, int port) {
    this.id = new ServerID(port, "0.0.0.0");
    this.vertx = (VertxInternal) vertx;
    this.builder = NettyServerBuilder.forPort(port);
  }

  private VertxServerBuilder(Vertx vertx, SocketAddress address) {
    this.id = new ServerID(((InetSocketAddress) address).getPort(), ((InetSocketAddress) address).getHostString());
    this.vertx = (VertxInternal) vertx;
    this.builder = NettyServerBuilder.forAddress(address);
  }

  /**
   * @return the underlying {@code NettyServerBuilder}
   */
  public NettyServerBuilder nettyBuilder() {
    return builder;
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

  @Override
  public VertxServerBuilder intercept(ServerInterceptor interceptor) {
    builder.intercept(interceptor);
    return this;
  }

  @Override
  public VertxServerBuilder useTransportSecurity(InputStream certChain, InputStream privateKey) {
    builder.useTransportSecurity(certChain, privateKey);
    return this;
  }

  @Override
  public VertxServerBuilder handshakeTimeout(long timeout, TimeUnit unit) {
    builder.handshakeTimeout(timeout, unit);
    return this;
  }

  @Override
  public VertxServerBuilder maxInboundMessageSize(int bytes) {
    builder.maxInboundMessageSize(bytes);
    return this;
  }

  @Override
  public VertxServerBuilder setBinaryLog(BinaryLog binaryLog) {
    builder.setBinaryLog(binaryLog);
    return this;
  }

  @Override
  public VertxServerBuilder maxInboundMetadataSize(int bytes) {
    builder.maxInboundMetadataSize(bytes);
    return this;
  }

  public VertxServerBuilder useSsl(Handler<HttpServerOptions> handler) {
    handler.handle(options);
    return this;
  }

  /**
   * Add a command decorator for the grpc calls.
   * The decorator provides a way to invoke arbitrary code before handling of the grpc request starts
   *
   * @param commandDecorator the decorator
   * @return this
   */
  public VertxServerBuilder commandDecorator(Consumer<Runnable> commandDecorator) {
    this.commandDecorator = commandDecorator;
    return this;
  }

  public VertxServer build() {
    ContextInternal context = vertx.getOrCreateContext();
    return new VertxServer(id, options, builder, context, commandDecorator);
  }
}
