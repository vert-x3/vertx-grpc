package io.vertx.ext.grpc;

import examples.HelloReply;
import examples.HelloRequest;
import examples.VertxGreeterGrpc;
import io.grpc.ManagedChannel;
import io.vertx.core.Context;
import io.vertx.core.Future;
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

  @Test(timeout = 10_000L)
  public void testCommandDecoration(TestContext should) {
    final Async test = should.async();

    TestDecorator decorator = new TestDecorator();

    server = VertxServerBuilder.forPort(vertx, port)
      .commandDecorator(decorator)
      .addService(new VertxGreeterGrpc.GreeterVertxImplBase() {
        @Override
        public Future<HelloReply> sayHello(HelloRequest request) {
          should.assertTrue(Context.isOnEventLoopThread());

          return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        }
      }).build();

    server.start(ar -> {
      if (ar.succeeded()) {
        if (server.getRawServer() == null) {
          should.fail("The underlying server not exposed (server.getRawServer())");
        }

        ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();

        stub.sayHello(request)
          .onFailure(should::fail)
          .onSuccess(helloReply -> {
            should.assertTrue(Context.isOnEventLoopThread());
            should.assertEquals("Hello Julien", helloReply.getMessage());

            if (!decorator.invoked) {
              should.fail("Command Decorator was not invoked");
            }
            channel.shutdown();
            test.complete();
          });
      } else {
        should.fail(ar.cause());
      }
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
