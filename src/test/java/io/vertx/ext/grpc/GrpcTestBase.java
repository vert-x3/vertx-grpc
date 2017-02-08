package io.vertx.ext.grpc;

import io.grpc.BindableService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public abstract class GrpcTestBase {

  /* The port on which the server should run */
  Vertx vertx;
  int port;
  private VertxServer server;

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

  void startServer(BindableService service) throws Exception {
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

  void startServer(BindableService service, Handler<AsyncResult<Void>> completionHandler) {
    startServer(service, VertxServerBuilder.forPort(vertx, port), completionHandler);
  }

  void startServer(BindableService service, VertxServerBuilder builder, Handler<AsyncResult<Void>> completionHandler) {
    server = builder
        .addService(service)
        .build()
        .start(completionHandler);
  }
}
