package io.vertx.ext.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.vertx.core.*;
import io.vertx.core.internal.VertxInternal;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public abstract class GrpcTestBase {

  @Rule
  public RunTestOnContext rule = new RunTestOnContext(new VertxOptions().setEventLoopPoolSize(1));

  /* The port on which the server should run */
  Vertx vertx;
  int port;
  protected VertxServer server;

  @Before
  public void setUp() {
    port = 8080;
    vertx = rule.vertx();
  }

  @After
  public void tearDown(TestContext should) {
    final Async test = should.async();

    if (server != null) {
      VertxServer s = server;
      server = null;
      final long timerId = rule.vertx().setTimer(10_000L, t -> should.fail("Timeout shutting down"));
      Promise<Void> promise = Promise.promise();
      promise.future().onComplete(shutdown -> {
        rule.vertx().cancelTimer(timerId);
        if (shutdown.failed()) {
          should.fail(shutdown.cause());
        } else {
          test.complete();
        }
      });
      s.shutdown(promise);
    }
  }

  Future<Void> startServer(BindableService service) {
    return startServer(service, VertxServerBuilder.forPort(vertx, port));
  }

  Future<Void> startServer(BindableService service, VertxServerBuilder builder) {
    Promise<Void> promise = Promise.promise();
    server = builder
        .addService(service)
        .build()
        .start(promise);

    return promise.future();
  }

  Future<Void> startServer(ServerServiceDefinition service) {
    Promise<Void> promise = Promise.promise();
    startServer(service,promise);
    return promise.future();
  }

  void startServer(ServerServiceDefinition service, Completable<Void> completionHandler) {
    startServer(service, VertxServerBuilder.forPort(vertx, port), completionHandler);
  }

  void startServer(ServerServiceDefinition service, VertxServerBuilder builder, Completable<Void> completionHandler) {
    server = builder
      .addService(service)
      .build()
      .start(completionHandler);
  }
}
