package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.vertx.core.*;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SslTest extends GrpcTestBase {

  @Test
  public void testSimple(TestContext ctx) throws Exception {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v -> {
      startServer(new GreeterGrpc.GreeterVertxImplBase() {
        @Override
        public void sayHello(HelloRequest req, Handler<AsyncResult<HelloReply>> handler) {
          ctx.assertEquals(serverCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
          handler.handle(Future.succeededFuture(reply));
        }
      }, VertxServerBuilder.forPort(vertx, port)
          .useSsl(options -> options
              .setSsl(true)
              .setUseAlpn(true)
              .setKeyStoreOptions(new JksOptions()
                  .setPath("tls/server-keystore.jks")
                  .setPassword("wibble")))
          , ar -> {
        if (ar.succeeded()) {
          started.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      ManagedChannel channel = VertxChannelBuilder.
          forAddress(vertx, "localhost", port)
          .useSsl(options -> options.setSsl(true)
              .setUseAlpn(true)
              .setTrustStoreOptions(new JksOptions()
                  .setPath("tls/client-truststore.jks")
                  .setPassword("wibble")))
          .build();
      GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, ar -> {
        if (ar.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", ar.result().getMessage());
          async.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    });
  }
}
