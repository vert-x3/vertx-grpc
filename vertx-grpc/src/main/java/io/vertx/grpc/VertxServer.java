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

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.tls.SslContextManager;
import io.vertx.core.internal.tls.SslContextProvider;
import io.vertx.core.net.impl.NetServerImpl;
import io.vertx.core.net.impl.ServerID;
import io.vertx.core.net.impl.VertxEventLoopGroup;
import io.vertx.core.spi.transport.Transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    final Server server;
    final ThreadLocal<List<ContextInternal>> contextLocal = new ThreadLocal<>();

    private ActualServer(VertxInternal vertx,
                         ServerID id,
                         HttpServerOptions options,
                         NettyServerBuilder builder,
                         Consumer<Runnable> commandDecorator) {

      // SSL
      if (options.isSsl()) {
        ContextInternal other = vertx.createWorkerContext();
        SslContextProvider provider;
        try {
          SslContextManager helper = new SslContextManager(SslContextManager.resolveEngineOptions(options.getSslEngineOptions(), true));
          provider = helper
            .resolveSslContextProvider(options.getSslOptions(), "", options.getClientAuth(), Collections.singletonList(HttpVersion.HTTP_2.alpnName()), other)
            .toCompletionStage()
            .toCompletableFuture()
            .toCompletableFuture().get(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          throw new VertxException(e);
        } catch (ExecutionException e) {
          throw new VertxException(e.getCause());
        } catch (TimeoutException e) {
          throw new VertxException(e);
        }
        SslContext ctx = provider.createContext(true, true);
        builder.sslContext(ctx);
      }

      Transport transport = vertx.transport();

      this.id = id;
      this.options = options;
      Executor executor;
      if (commandDecorator == null) {
        executor = command -> contextLocal.get().get(0).runOnContext(event -> command.run());
      }else{
        executor = command -> contextLocal.get().get(0).runOnContext(event -> commandDecorator.accept(command));
      }
      this.server = builder
          .executor(executor)
          .channelFactory(transport.serverChannelFactory(false))
          .bossEventLoopGroup(vertx.getAcceptorEventLoopGroup())
          .workerEventLoopGroup(group)
          .build();
    }

    void start(ContextInternal context, Completable<Void> completionHandler) {
      boolean start = count.getAndIncrement() == 0;
      context.runOnContext(v -> {
        if (contextLocal.get() == null) {
          contextLocal.set(new ArrayList<>());
        }
        group.addWorker(context.nettyEventLoop());
        contextLocal.get().add(context);
        if (start) {
          context.<Void>executeBlocking(() -> {
            server.start();
            return null;
          }).onComplete(completionHandler);
        } else {
          completionHandler.succeed();
        }
      });
    }

    void stop(ContextInternal context, Promise<Void> promise) {
      boolean shutdown = count.decrementAndGet() == 0;
      context.runOnContext(v -> {
        group.removeWorker(context.nettyEventLoop());
        contextLocal.get().remove(context);
        if (shutdown) {
          map.remove(id);
          context.<Void>executeBlocking(() -> {
            server.shutdown();
            return null;
          }).onComplete(promise);
        } else {
          promise.complete();
        }
      });
    }

  }

  private final ServerID id;
  private final NettyServerBuilder builder;
  private final HttpServerOptions options;
  private ActualServer actual;
  private final ContextInternal context;
  private final Consumer<Runnable> commandDecorator;
  private Closeable hook;

  VertxServer(ServerID id,
              HttpServerOptions options,
              NettyServerBuilder builder,
              ContextInternal context,
              Consumer<Runnable> commandDecorator) {
    this.id = id;
    this.options = options;
    this.builder = builder;
    this.context = context;
    this.commandDecorator = commandDecorator;
  }

  @Override
  public VertxServer start() throws IOException {
    return start((res, err) -> {});
  }

  public VertxServer start(Completable<Void> completionHandler) {
    if (id.port() > 0) {
      actual = map.computeIfAbsent(id, id -> new ActualServer(context.owner(), id, options, builder, commandDecorator));
    } else {
      actual = new ActualServer(context.owner(), id, options, builder, commandDecorator);
    }
    actual.start(context, (res, err) -> {
      if (err == null) {
        hook = this::shutdown;
        context.addCloseHook(hook);
      }
      completionHandler.complete(res, err);
    });
    return this;
  }

  @Override
  public VertxServer shutdown() {
    return shutdown(Promise.promise());
  }

  public VertxServer shutdown(Promise<Void> completionHandler) {
    if (hook != null) {
      context.removeCloseHook(hook);
    }
    PromiseInternal<Void> promise = context.promise(completionHandler);
    actual.stop(context, promise);
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

  public Server getRawServer() {
    return actual.server;
  }
}
