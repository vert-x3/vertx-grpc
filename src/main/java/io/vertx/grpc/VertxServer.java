package io.vertx.grpc;

import io.grpc.Server;
import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyServerBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.net.impl.HandlerManager;
import io.vertx.core.net.impl.ServerID;
import io.vertx.core.net.impl.VertxEventLoopGroup;

import java.io.IOException;
import java.util.ArrayList;
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
    final AtomicInteger count = new AtomicInteger();
    final VertxEventLoopGroup group = new VertxEventLoopGroup();
    final HandlerManager<String> manager = new HandlerManager<>(group);
    final ServerImpl server;
    final ThreadLocal<List<ContextImpl>> contextLocal = new ThreadLocal<>();

    private ActualServer(ServerID id, NettyServerBuilder builder) {
      this.id = id;
      this.server = builder
          .executor(command -> {
            contextLocal.get().get(0).executeFromIO(command::run);
          })
          .workerEventLoopGroup(group)
          .build();
    }

    void start(ContextImpl context, Handler<AsyncResult<Void>> completionHandler) {
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

    void stop(ContextImpl context, Handler<AsyncResult<Void>> completionHandler) {
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
  private ActualServer actual;
  private final ContextImpl context;

  VertxServer(ServerID id, NettyServerBuilder builder, ContextImpl context) {
    this.id = id;
    this.builder = builder;
    this.context = context;
  }

  @Override
  public VertxServer start() throws IOException {
    return start(ar -> {});
  }

  public VertxServer start(Handler<AsyncResult<Void>> completionHandler) {
    actual = map.computeIfAbsent(id, id -> new ActualServer(id, builder));
    actual.start(context, completionHandler);
    return this;
  }

  @Override
  public VertxServer shutdown() {
    return shutdown(ar -> {});
  }

  public VertxServer shutdown(Handler<AsyncResult<Void>> completionHandler) {
    actual.stop(context, completionHandler);
    return this;
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
