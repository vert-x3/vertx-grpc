package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.StreamHelper;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class ScaleTest {

  private static final Set<Thread> threads = Collections.synchronizedSet(new HashSet<>());
  private static int port = 50051;

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

    private VertxServer server;
    private GreeterGrpc.GreeterImplBase service;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
      service = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
          threads.add(Thread.currentThread());
          HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
          responseObserver.onNext(reply);
          responseObserver.onCompleted();
        }
      };
      server = VertxServerBuilder.forPort(vertx, port).addService(service).build();
      server.start(startFuture.completer());
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
      server.shutdown(stopFuture.completer());
    }
  }

  @Test
  public void testScale(TestContext ctx) throws Exception {
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
      ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext(true)
          .build();
      GreeterGrpc.GreeterStub blockingStub = GreeterGrpc.newStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      blockingStub.sayHello(request, StreamHelper.future(ar -> {
        channel.shutdown();
        if (ar.succeeded()) {
          ctx.assertEquals("Hello Julien", ar.result().getMessage());
          async.countDown();
          if (async.count() == 0) {
            ctx.assertEquals(2, threads.size());
          }
        } else {
          ctx.fail(ar.cause());
        }
      }));
    }
  }
}
