package io.vertx.ext.grpc;

import examples.GreeterGrpc;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;
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
    VertxServerBuilder.forPort(vertx, 0)
      .addService(new GreeterGrpc.GreeterImplBase() { })
      .build()
      .start(ctx.asyncAssertSuccess());
  }

  private void assumeNativeTransport() {
    Transport nativeTransport = VertxBuilder.nativeTransport();
    Assume.assumeTrue(nativeTransport != null && nativeTransport.isAvailable());
  }
}
