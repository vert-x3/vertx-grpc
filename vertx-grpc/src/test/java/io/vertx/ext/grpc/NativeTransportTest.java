package io.vertx.ext.grpc;

import examples.GreeterGrpc;
import io.vertx.core.*;
import io.vertx.core.impl.VertxBootstrapImpl;
import io.vertx.core.spi.transport.Transport;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class NativeTransportTest {

  @Test
  public void testNativeTransportEnabled(TestContext ctx) {
    assumeNativeTransport();
    testInternal(ctx, Vertx.vertx(new VertxOptions().setPreferNativeTransport(true)));
  }

  @Test
  public void testNativeTransportDisabled(TestContext ctx) {
    assumeNativeTransport();
    testInternal(ctx, Vertx.vertx(new VertxOptions().setPreferNativeTransport(false)));
  }

  private void testInternal(TestContext ctx, Vertx vertx) {
    Handler<AsyncResult<Void>> latch = ctx.asyncAssertSuccess();
    VertxServerBuilder.forPort(vertx, 0)
      .addService(new GreeterGrpc.GreeterImplBase() { })
      .build()
      .start((result, failure) -> {
        if (failure != null) {
          latch.handle(Future.failedFuture(failure));
        } else {
          latch.handle(Future.succeededFuture());
        }
      });
  }

  private void assumeNativeTransport() {
    Transport nativeTransport = VertxBootstrapImpl.nativeTransport();
    Assume.assumeTrue(nativeTransport != null && nativeTransport.isAvailable());
  }
}
