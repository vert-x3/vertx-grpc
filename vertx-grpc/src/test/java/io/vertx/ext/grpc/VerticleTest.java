package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class VerticleTest {

  private static final Set<Thread> threads = Collections.synchronizedSet(new HashSet<>());

  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  /* The port on which the server should run */
  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = rule.vertx();
  }

  @After
  public void tearDown() {
    threads.clear();
  }

  public static class GrpcVerticle extends AbstractVerticle {

    private final int port;
    private volatile VertxServer server;

    public GrpcVerticle(int port) {
      this.port = port;
    }

    public GrpcVerticle() {
      this(50051);
    }

    @Override
    public void start(Promise<Void> startFuture) throws Exception {
      VertxGreeterGrpc.GreeterVertxImplBase service = new VertxGreeterGrpc.GreeterVertxImplBase() {
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
    public void stop(Promise<Void> stopFuture) {
      server.shutdown(stopFuture);
    }
  }

  @Test(timeout = 10_000L)
  public void testScaleVerticle(TestContext should) {
    final int num = 10;
    final Async test = should.async(num);
    vertx.deployVerticle(GrpcVerticle.class.getName(), new DeploymentOptions().setInstances(2))
      .onFailure(should::fail)
      .onSuccess(id -> {
        List<ManagedChannel> toClose = new ArrayList<>();
        try {
          for (int i = 0; i < num; i++) {
            ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
              .usePlaintext()
              .build();
            toClose.add(channel);
            VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
            HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
            stub.sayHello(request).onComplete(should.asyncAssertSuccess(res -> {
              should.assertEquals("Hello Julien", res.getMessage());
              test.countDown();
              if (test.count() == 0) {
                should.assertEquals(2, threads.size());
              }
            }));
          }
        } finally {
          toClose.forEach(ManagedChannel::shutdown);
        }
      });
  }

  @Test(timeout = 10_000L)
  public void testCloseInVerticle(TestContext should) {
    Async test = should.async();
    vertx.deployVerticle(GrpcVerticle.class.getName())
      .onFailure(should::fail)
      .onSuccess(id -> {
        vertx.undeploy(id)
          .onFailure(should::fail)
          .onSuccess(v -> {
            ManagedChannel channel= VertxChannelBuilder.forAddress(vertx, "localhost", 50051)
              .usePlaintext()
              .build();

            try {
              VertxGreeterGrpc.GreeterVertxStub blockingStub = VertxGreeterGrpc.newVertxStub(channel);
              HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
              blockingStub.sayHello(request)
                .onSuccess(res -> should.fail("Verticle is undeployed, should not reach here"))
                .onFailure(err -> test.complete());
            } finally {
              channel.shutdown();
            }
          });
      });
  }

  @Test(timeout = 10_000L)
  public void testBilto(TestContext should) {
    final Async test = should.async();
    List<GrpcVerticle> verticles = Collections.synchronizedList(new ArrayList<>());

    vertx.deployVerticle(() -> {
      GrpcVerticle verticle = new GrpcVerticle(0);
      verticles.add(verticle);
      return verticle;
    }, new DeploymentOptions().setInstances(2))
      .onFailure(should::fail)
      .onSuccess(id -> {
        should.assertEquals(2, verticles.size());
        should.assertNotEquals(verticles.get(0).server.getPort(), verticles.get(1).server.getPort());
        test.complete();
      });

  }
}
