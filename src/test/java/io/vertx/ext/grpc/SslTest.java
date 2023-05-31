package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.ClientOptionsBase;
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
  public void testConnect(TestContext ctx) throws Exception {
    testSimple(options -> options.setSsl(true)
      .setUseAlpn(true)
      .setTrustStoreOptions(new JksOptions()
        .setPath("tls/client-truststore.jks")
        .setPassword("wibble")), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyStoreOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble")), ctx, true);
  }

  @Test
  public void testTrustAll(TestContext ctx) throws Exception {
    testSimple(options -> options
      .setTrustAll(true)
      .setSsl(true)
      .setUseAlpn(true), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyStoreOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble")), ctx, true);
  }

  @Test
  public void testClientAuth(TestContext ctx) throws Exception {
    testSimple(options -> options
      .setTrustAll(true)
      .setSsl(true)
      .setUseAlpn(true)
      .setKeyStoreOptions(new JksOptions()
        .setPath("tls/server-keystore.jks")
        .setPassword("wibble"))
      , options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyStoreOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble"))
        .setClientAuth(ClientAuth.REQUIRED)
        .setTrustStoreOptions(new JksOptions()
          .setPath("tls/client-truststore.jks")
          .setPassword("wibble")), ctx, true);
  }

  @Test
  public void testClientAuthFail(TestContext ctx) throws Exception {
    testSimple(options -> options
      .setTrustAll(true)
      .setSsl(true)
      .setUseAlpn(true), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyStoreOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble"))
        .setClientAuth(ClientAuth.REQUIRED)
        .setTrustStoreOptions(new JksOptions()
          .setPath("tls/client-truststore.jks")
          .setPassword("wibble")), ctx, false);
  }

  private void testSimple(Handler<ClientOptionsBase> clientSslBuilder,
                          Handler<HttpServerOptions> serverSslBuilder,
                          TestContext ctx,
                          boolean pass) throws Exception {

    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v -> {
      GreeterGrpc.GreeterVertxImplBase service = new GreeterGrpc.GreeterVertxImplBase() {
        @Override
        public void sayHello(HelloRequest req, Future<HelloReply> future) {
          ctx.assertEquals(serverCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
        }
      };
      startServer(service, VertxServerBuilder.forPort(vertx, port)
          .useSsl(serverSslBuilder)
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
          .useSsl(clientSslBuilder)
          .build();
      GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, ar -> {
        if (ar.succeeded()) {
          if (pass) {
            ctx.assertEquals(clientCtx, Vertx.currentContext());
            ctx.assertTrue(Context.isOnEventLoopThread());
            ctx.assertEquals("Hello Julien", ar.result().getMessage());
            async.complete();
          } else {
            ctx.fail("Expected failure");
          }
        } else {
          if (pass) {
            ctx.fail(ar.cause());
          } else {
            async.complete();
          }
        }
        channel.shutdown();
      });
    });
  }
}
