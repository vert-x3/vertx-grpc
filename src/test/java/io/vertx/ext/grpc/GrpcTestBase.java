package io.vertx.ext.grpc;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.StreamObserver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.StreamHelper;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public abstract class GrpcTestBase {

  /* The port on which the server should run */
  protected Vertx vertx;
  protected int port;
  protected VertxServer server;

  @Before
  public void setUp() {
    port = 50051;
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      VertxServer s = server;
      server = null;
      CountDownLatch fut = new CountDownLatch(1);
      s.shutdown(ar -> fut.countDown());
      fut.await(10, TimeUnit.SECONDS);
    }
    CountDownLatch latch = new CountDownLatch(1);
    vertx.close(ar -> latch.countDown());
    latch.await(10, TimeUnit.SECONDS);
  }

  protected void startServer(BindableService service) throws Exception {
    CompletableFuture<Void> fut = new CompletableFuture<>();
    startServer(service, ar -> {
      if (ar.succeeded()) {
        fut.complete(null);
      } else {
        fut.completeExceptionally(ar.cause());
      }
    });
    fut.get(10, TimeUnit.SECONDS);
  }

  protected void startServer(BindableService service, Handler<AsyncResult<Void>> completionHandler) {
    startServer(service, VertxServerBuilder.forPort(vertx, port), completionHandler);
  }

  protected void startServer(BindableService service, VertxServerBuilder builder, Handler<AsyncResult<Void>> completionHandler) {
    server = builder
        .addService(service)
        .build()
        .start(completionHandler);
  }
}
