package io.vertx.grpc;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.DelegatingSslContext;
import io.netty.handler.ssl.SslContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Closeable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.impl.HandlerManager;
import io.vertx.core.net.impl.SSLHelper;
import io.vertx.core.net.impl.ServerID;
import io.vertx.core.net.impl.VertxEventLoopGroup;
import io.vertx.core.net.impl.transport.Transport;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxServer extends Server {

  private static final ConcurrentMap<ServerID, ActualServer> map = new ConcurrentHashMap<>();

  private static class ActualServer {

    final ServerID id;
    final HttpServerOptions options;
    final AtomicInteger count = new AtomicInteger();
    final VertxEventLoopGroup group = new VertxEventLoopGroup();
    final HandlerManager<String> manager = new HandlerManager<>(group);
    final Server server;
    final ThreadLocal<List<ContextInternal>> contextLocal = new ThreadLocal<>();

    private ActualServer(Vertx vertx, ServerID id, HttpServerOptions options, NettyServerBuilder builder) {

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

      this.id = id;
      this.options = options;
      this.server = builder
          .executor(command -> {
            contextLocal.get().get(0).executeFromIO(event -> command.run());
          })
          .channelFactory(transport.serverChannelFactory(false))
          .bossEventLoopGroup(group)
          .workerEventLoopGroup(group)
          .build();
    }

    void start(ContextInternal context, Handler<AsyncResult<Void>> completionHandler) {
      boolean start = count.getAndIncrement() == 0;
      context.runOnContext(v -> {
        if (contextLocal.get() == null) {
          contextLocal.set(new ArrayList<>());
        }
        manager.addHandler("foo", context);
        contextLocal.get().add(context);
        if (start) {
          context.executeBlocking(v2 -> {
            try {
              server.start();
              v2.complete();
            } catch (IOException e) {
              v2.fail(e);
            }
          }, completionHandler);
        } else {
          completionHandler.handle(Future.succeededFuture());
        }
      });
    }

    void stop(ContextInternal context, Handler<AsyncResult<Void>> completionHandler) {
      boolean shutdown = count.decrementAndGet() == 0;
      context.runOnContext(v -> {
        manager.removeHandler("foo", context);
        contextLocal.get().remove(context);
        if (shutdown) {
          map.remove(id);
          server.shutdown();
        }
        completionHandler.handle(Future.succeededFuture());
      });
    }

  }

  private final ServerID id;
  private final NettyServerBuilder builder;
  private final HttpServerOptions options;
  private ActualServer actual;
  private final ContextInternal context;
  private Closeable hook;

  VertxServer(ServerID id, HttpServerOptions options, NettyServerBuilder builder, ContextInternal context) {
    this.id = id;
    this.options = options;
    this.builder = builder;
    this.context = context;
  }

  @Override
  public VertxServer start() throws IOException {
    return start(ar -> {});
  }

  public VertxServer start(Handler<AsyncResult<Void>> completionHandler) {
    if (id.port > 0) {
      actual = map.computeIfAbsent(id, id -> new ActualServer(context.owner(), id, options, builder));
    } else {
      actual = new ActualServer(context.owner(), id, options, builder);
    }
    actual.start(context, ar1 -> {
      if (ar1.succeeded()) {
        hook = ar2 -> shutdown(ar2);
        context.addCloseHook(hook);
      }
      completionHandler.handle(ar1);
    });
    return this;
  }

  @Override
  public VertxServer shutdown() {
    return shutdown(ar -> {});
  }

  public VertxServer shutdown(Handler<AsyncResult<Void>> completionHandler) {
    if (hook != null) {
      context.removeCloseHook(hook);
    }
    actual.stop(context, completionHandler);
    return this;
  }

  @Override
  public int getPort() {
    return actual.server.getPort();
  }

  @Override
  public VertxServer shutdownNow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isShutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTerminated() {
    return actual.server.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return actual.server.awaitTermination(timeout, unit);
  }

  @Override
  public void awaitTermination() throws InterruptedException {
    actual.server.awaitTermination();
  }
}
