package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SslTest extends GrpcTestBase {

  @Test
  public void testConnect(TestContext ctx) throws Exception {
    testSimple(options -> options.setSsl(true)
      .setUseAlpn(true)
      .setTrustOptions(new JksOptions()
        .setPath("tls/client-truststore.jks")
        .setPassword("wibble")), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble")), ctx, true);
  }

  @Test(timeout = 10_000L)
  public void testTrustAll(TestContext ctx) throws Exception {
    testSimple(options -> options
      .setTrustAll(true)
      .setSsl(true)
      .setUseAlpn(true), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble")), ctx, true);
  }

  @Test(timeout = 10_000L)
  public void testClientAuth(TestContext ctx) throws Exception {
    testSimple(options -> options
        .setTrustAll(true)
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble"))
      , options ->
        options
          .setSsl(true)
          .setUseAlpn(true)
          .setKeyCertOptions(new JksOptions()
            .setPath("tls/server-keystore.jks")
            .setPassword("wibble"))
          .setClientAuth(ClientAuth.REQUIRED)
          .setTrustOptions(new JksOptions()
            .setPath("tls/client-truststore.jks")
            .setPassword("wibble")), ctx, true);
  }

  @Test(timeout = 10_000L)
  public void testClientAuthFail(TestContext ctx) throws Exception {
    testSimple(options -> options
      .setTrustAll(true)
      .setSsl(true)
      .setUseAlpn(true), options ->
      options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("tls/server-keystore.jks")
          .setPassword("wibble"))
        .setClientAuth(ClientAuth.REQUIRED)
        .setTrustOptions(new JksOptions()
          .setPath("tls/client-truststore.jks")
          .setPassword("wibble")), ctx, false);
  }

  private void testSimple(Handler<ClientOptionsBase> clientSslBuilder,
                          Handler<HttpServerOptions> serverSslBuilder,
                          TestContext should,
                          boolean pass) {

    final Async test = should.async();

    VertxGreeterGrpc.GreeterVertxImplBase service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        should.assertTrue(Context.isOnEventLoopThread());
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    startServer(service, VertxServerBuilder.forPort(vertx, port).useSsl(serverSslBuilder))
      .onFailure(should::fail)
      .onSuccess(v -> {

        AtomicReference<ManagedChannel> channelRef = new AtomicReference<>();
        try {
          ManagedChannel channel = null;
          try {
            channel = VertxChannelBuilder.
              forAddress(vertx, "localhost", port)
              .useSsl(clientSslBuilder)
              .build();
          } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
          }

          channelRef.set(channel);
            VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
            HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
            Future<HelloReply> fut = stub.sayHello(request);

            if (pass) {
              fut.onComplete(should.asyncAssertSuccess(res -> {
                should.assertTrue(Context.isOnEventLoopThread());
                should.assertEquals("Hello Julien", res.getMessage());
                test.complete();
              }));
            } else {
              fut.onComplete(should.asyncAssertFailure(err -> test.complete()));
            }

        } finally {
          channelRef.get().shutdown();
        }
      });
  }
}
