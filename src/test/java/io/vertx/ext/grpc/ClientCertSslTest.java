package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ClientCertSslTest extends GrpcTestBase  {
  private static final Context.Key<String> SSL_COMMON_NAME = Context.key("SSLCOMMONNAME");

  @Test
  public void testClientCert(TestContext ctx) throws Exception {
    Async started = ctx.async();
    io.vertx.core.Context serverCtx = vertx.getOrCreateContext();
    BindableService service = new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest req, Future<HelloReply> future) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(io.vertx.core.Context.isOnEventLoopThread());
        future.complete(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
      }
    };
    ServerServiceDefinition sd = ServerInterceptors.intercept(service,
      new ServerInterceptor() {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                                                                     ServerCallHandler<ReqT, RespT> serverCallHandler) {
          System.out.println("We're in");
          SSLSession sslSession = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
          if (sslSession == null) {
            throw new IllegalArgumentException("No ssl session");
          }

          String cn = "";
          try {
            Certificate[] certs = sslSession.getPeerCertificates();
            System.out.println("Certs:"+certs.length);
            Principal principal = sslSession.getPeerPrincipal();
            if (!(principal instanceof X500Principal)) {
              throw new IllegalArgumentException("Not authenticated");
            }
            X500Principal x509principal = (X500Principal)principal;
            Pattern p = Pattern.compile("(^|,)CN=([^,]*)(,|$)");
            Matcher matcher = p.matcher(x509principal.getName());
            if(matcher.find()) cn = matcher.group(2);
            System.out.println("CN:"+cn);
          } catch (SSLPeerUnverifiedException e) {
            System.out.println("Peer is not verified:"+e.getMessage());
            throw new IllegalArgumentException("Peer is not verified:"+e.getMessage());
          }
          return Contexts.interceptCall(
            Context.current().withValue(SSL_COMMON_NAME, cn), serverCall, metadata, serverCallHandler);
        }
      });

    serverCtx.runOnContext(v -> startServer(sd, VertxServerBuilder.forPort(vertx,port)
        .useSsl(options -> {
          options
            .setSsl(true)
            .setUseAlpn(true)
            .setKeyStoreOptions(new JksOptions()
              .setPath("tls/server-keystore-clientcert.jks")
              .setPassword("testpw"));
          PemTrustOptions trustOptions = new PemTrustOptions()
            .addCertPath("tls/TestCA.crt");
          HttpServerOptions sslOptions = (HttpServerOptions)options;
          sslOptions.setClientAuth(ClientAuth.REQUIRED)
            .setTrustOptions(trustOptions);

        })
      , ar -> {
        if (ar.succeeded()) {
          started.complete();
        } else {
          ctx.fail(ar.cause());
        }
      }));
    started.awaitSuccess(10000);
    System.out.println("Running");
    Async async = ctx.async();
    io.vertx.core.Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      ManagedChannel channel = VertxChannelBuilder.
        forAddress(vertx, "localhost", port)
        .useSsl(options -> {
          PemTrustOptions trustOptions = new PemTrustOptions()
            .addCertPath("tls/TestCA.crt");
          options.setSsl(true)
            .setUseAlpn(true)
            .setPemTrustOptions(trustOptions)
            .setPemKeyCertOptions(new PemKeyCertOptions().addKeyPath("tls/TestClient.p8")
            .addCertPath("tls/TestClient.crt"));
        })
        .build();
      GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, ar -> {
        if (ar.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(io.vertx.core.Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", ar.result().getMessage());
          async.complete();
        } else {
          ctx.fail(ar.cause());
        }
      });
    });

  }


}
