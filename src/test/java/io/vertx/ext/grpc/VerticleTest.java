package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
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
    private GreeterGrpc.GreeterImplBase service;

    public GrpcVerticle(int port) {
      this.port = port;
    }

    public GrpcVerticle() {
      this(50051);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
      service = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
          threads.add(Thread.currentThread());
          responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + req.getName()).build());
          responseObserver.onCompleted();
        }
      };
      server = VertxServerBuilder.forPort(vertx, port).addService(service).build();
      server.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
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
    for (int i = 0;i < num;i++) {
      ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
          .usePlaintext(true)
          .build();
      GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request, new StreamObserver<HelloReply>() {
        private HelloReply result;
        @Override
        public void onNext(HelloReply helloReply) {
          result = helloReply;
        }
        @Override
        public void onError(Throwable throwable) {
          ctx.fail(throwable);
        }
        @Override
        public void onCompleted() {
          ctx.assertEquals("Hello Julien", result.getMessage());
          async.countDown();
          if (async.count() == 0) {
            ctx.assertEquals(2, threads.size());
          }
        }
      });
    }
  }

  @Test
  public void testCloseInVerticle(TestContext ctx) throws Exception {
    Async started = ctx.async();
    vertx.deployVerticle(GrpcVerticle.class.getName(), ar1 -> {
      if (ar1.succeeded()) {
        vertx.undeploy(ar1.result(), ar2 -> {
          if (ar2.succeeded()) {
            started.complete();
          } else {
            ctx.fail(ar2.cause());
          }
        });
      } else {
        ctx.fail(ar1.cause());
      }
    });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
        .usePlaintext(true)
        .build();
    GreeterGrpc.GreeterStub blockingStub = GreeterGrpc.newStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    blockingStub.sayHello(request, new StreamObserver<HelloReply>() {
      @Override
      public void onNext(HelloReply helloReply) {

      }
      @Override
      public void onError(Throwable throwable) {
        async.complete();
        channel.shutdown();
      }
      @Override
      public void onCompleted() {
        ctx.fail();
      }
    });
  }

  @Test
  public void testBilto(TestContext ctx) throws Exception {
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
