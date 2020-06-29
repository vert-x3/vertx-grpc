package io.vertx.ext.grpc;

import examples.HelloReply;
import examples.HelloRequest;
import examples.VertxGreeterGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 4/28/20
 */
public class CommandDecoratorTest extends GrpcTestBase {
  @Test
  public void testCommandDecoration(TestContext ctx) {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();

    TestDecorator decorator = new TestDecorator();

    serverCtx.runOnContext(
      v -> {
        /*    server = builder
        .addService(service)
        .build()
        .start(completionHandler);*/
        server = VertxServerBuilder.forPort(vertx, port)
          .commandDecorator(decorator)
          .addService(new VertxGreeterGrpc.GreeterImplBase() {
            @Override
            public Future<HelloReply> sayHello(HelloRequest request) {
              ctx.assertEquals(serverCtx, Vertx.currentContext());
              ctx.assertTrue(Context.isOnEventLoopThread());

              return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
            }
          }).build();

        server.start(ar -> {
          if (ar.succeeded()) {
            started.complete();
          } else {
            ctx.fail(ar.cause());
          }
        });
      }
    );

    started.awaitSuccess(10000);

    if (server.getRawServer() == null) {
      ctx.fail("The underlying server not exposed (server.getRawServer())");
    }

    Async async = ctx.async();
    Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
        .usePlaintext(true)
        .build();
      VertxGreeterGrpc.VertxGreeterStub stub = VertxGreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request).onComplete(ar -> {
        if (ar.succeeded()) {
          ctx.assertEquals(clientCtx, Vertx.currentContext());
          ctx.assertTrue(Context.isOnEventLoopThread());
          ctx.assertEquals("Hello Julien", ar.result().getMessage());

          if (!decorator.invoked) {
            ctx.fail("Command Decorator was not invoked");
          }
          async.complete();
        } else {
          ctx.fail(ar.cause());
        }
        channel.shutdown();
      });
    });
  }


  private static class TestDecorator implements Consumer<Runnable> {
    private volatile boolean invoked = false;

    @Override
    public void accept(Runnable runnable) {
      invoked = true;
      runnable.run();
    }
  }
}
