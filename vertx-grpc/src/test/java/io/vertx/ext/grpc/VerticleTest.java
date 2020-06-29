package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class VerticleTest {

  private static final Set<Thread> threads = Collections.synchronizedSet(new HashSet<>());

  /* The port on which the server should run */
  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() throws Exception {
    threads.clear();
    CountDownLatch latch = new CountDownLatch(1);
    vertx.close(ar -> latch.countDown());
    latch.await(10, TimeUnit.SECONDS);
  }

  public static class GrpcVerticle extends AbstractVerticle {

    private final int port;
    private volatile VertxServer server;
    private VertxGreeterGrpc.GreeterVertxImplBase service;

    public GrpcVerticle(int port) {
      this.port = port;
    }

    public GrpcVerticle() {
      this(50051);
    }

    @Override
    public void start(Promise<Void> startFuture) throws Exception {
      service = new VertxGreeterGrpc.GreeterVertxImplBase() {
        @Override
        public Future<HelloReply> sayHello(HelloRequest request) {
          threads.add(Thread.currentThread());

          return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        }
      };
      server = VertxServerBuilder.forPort(vertx, port).addService(service).build();
      server.start(startFuture);
    }

    @Override
    public void stop(Promise<Void> stopFuture) throws Exception {
      server.shutdown(stopFuture);
    }
  }

  @Test
  public void testScaleVerticle(TestContext ctx) throws Exception {
    Async started = ctx.async();
    vertx.deployVerticle(GrpcVerticle.class.getName(), new DeploymentOptions().setInstances(2), ar -> {
      if (ar.succeeded()) {
        started.complete();
      } else {
        ctx.fail(ar.cause());
      }
    });
    started.awaitSuccess(10000);
    final int num = 10;
    Async async = ctx.async(num);
    List<ManagedChannel> toClose = new ArrayList<>();
    try {
      for (int i = 0;i < num;i++) {
        ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
            .usePlaintext(true)
            .build();
        toClose.add(channel);
        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
        stub.sayHello(request).onComplete(ctx.asyncAssertSuccess(res -> {
          ctx.assertEquals("Hello Julien", res.getMessage());
          async.countDown();
          if (async.count() == 0) {
            ctx.assertEquals(2, threads.size());
          }
        }));
      }
      async.awaitSuccess(20000);
    } finally {
      toClose.forEach(ManagedChannel::shutdown);
    }
  }

  @Test
  public void testCloseInVerticle(TestContext ctx) throws Exception {
    Async started = ctx.async();
    vertx.deployVerticle(GrpcVerticle.class.getName(), ctx.asyncAssertSuccess(id -> {
      vertx.undeploy(id, ctx.asyncAssertSuccess(v -> started.complete()));
    }));
    started.awaitSuccess(10000);
    Async async = ctx.async();
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
        .usePlaintext(true)
        .build();
    try {
      VertxGreeterGrpc.GreeterVertxStub blockingStub = VertxGreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      blockingStub.sayHello(request).onComplete(ctx.asyncAssertFailure(err -> async.complete()));
      async.awaitSuccess(20000);
    } finally {
      channel.shutdown();
    }
  }

  @Test
  public void testBilto(TestContext ctx) {
    Async started = ctx.async();
    List<GrpcVerticle> verticles = Collections.synchronizedList(new ArrayList<>());
    vertx.deployVerticle(() -> {
      GrpcVerticle verticle = new GrpcVerticle(0);
      verticles.add(verticle);
      return verticle;
    }, new DeploymentOptions().setInstances(2), ctx.asyncAssertSuccess(v -> started.complete()));
    started.awaitSuccess(10000);
    ctx.assertEquals(2, verticles.size());
    ctx.assertNotEquals(verticles.get(0).server.getPort(), verticles.get(1).server.getPort());
  }
}
