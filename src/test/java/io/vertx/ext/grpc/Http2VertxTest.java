package io.vertx.ext.grpc;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.StreamHelper;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.grpc.examples.helloworld.*;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class Http2VertxTest {

  /* The port on which the server should run */
  private Vertx vertx;
  private int port = 50051;
  private Server server;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
    }
    vertx.close();
  }

  private void startServer(BindableService service) throws Exception {
    server = VertxServerBuilder.forPort(vertx, port)
        .addService(service)
        .build()
        .start();
  }

  @Test
  public void testSimple(TestContext ctx) throws Exception {
    Async started = ctx.async();
    startServer(new GreeterGrpc.GreeterImplBase() {
      @Override
      public ServerServiceDefinition bindService() {
        ServerServiceDefinition sd = super.bindService();
        started.complete();
        return sd;
      }
      @Override
      public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        ctx.assertTrue(Context.isOnEventLoopThread());
        ctx.assertNotNull(Vertx.currentContext());
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }
    });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
    GreeterGrpc.GreeterStub blockingStub = GreeterGrpc.newStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    blockingStub.sayHello(request, StreamHelper.future(ar -> {
      if (ar.succeeded()) {
        ctx.assertEquals("Hello Julien", ar.result().getMessage());
        ctx.assertTrue(Context.isOnEventLoopThread());
        ctx.assertNotNull(Vertx.currentContext());
        async.complete();
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }
}
