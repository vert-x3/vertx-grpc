package io.vertx.ext.grpc;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerCallRequest;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public abstract class GrpcTestBase2 {

  /* The port on which the server should run */
  Vertx vertx;
  int port;
  protected VertxServer server;

  @Before
  public void setUp() {
    port = 8080;
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext should) {
    vertx.close(should.asyncAssertSuccess());
  }

  Future<Void> startServer(BindableService service) {
    return startServer(service, VertxServerBuilder.forPort(vertx, port));
  }

  Future<Void> startServer(BindableService service, VertxServerBuilder builder) {
    Promise<Void> promise = ((VertxInternal) vertx).promise();
    server = builder
        .addService(service)
        .build()
        .start(promise);

    return promise.future();
  }

  protected void startServer(GrpcServer server) {
    CompletableFuture<Void> res = new CompletableFuture<>();
    vertx.createHttpServer().requestHandler(server).listen(8080, "localhost")
      .onComplete(ar -> {
        if (ar.succeeded()) {
          res.complete(null);
        } else {
          res.completeExceptionally(ar.cause());
        }
      });
    try {
      res.get(20, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e);
      throw afe;
    } catch (ExecutionException e) {
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e.getCause());
      throw afe;
    } catch (TimeoutException e) {
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e);
      throw afe;
    }
  }

  Future<Void> startServer(ServerServiceDefinition service) {
    Promise<Void> promise = ((VertxInternal) vertx).promise();
    startServer(service, ar -> {
      if (ar.succeeded()) {
        promise.complete();
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  void startServer(ServerServiceDefinition service, Handler<AsyncResult<Void>> completionHandler) {
    startServer(service, VertxServerBuilder.forPort(vertx, port), completionHandler);
  }

  void startServer(ServerServiceDefinition service, VertxServerBuilder builder, Handler<AsyncResult<Void>> completionHandler) {
    server = builder
      .addService(service)
      .build()
      .start(completionHandler);
  }
}
