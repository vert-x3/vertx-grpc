package examples;

import io.grpc.ManagedChannel;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    VertxServer server = VertxServerBuilder.forAddress(vertx, "localhost", 8080).addService(new GreeterGrpc.GreeterVertxImplBase() {
      @Override
      public void sayHello(HelloRequest request, Handler<AsyncResult<HelloReply>> handler) {
        System.out.println("Hello" + request.getName());
        handler.handle(Future.succeededFuture(HelloReply.newBuilder().setMessage(request.getName()).build()));
      }
    }).build();
    server.start(asyncStart -> {
      if (asyncStart.succeeded()) {
        ManagedChannel channel = VertxChannelBuilder
            .forAddress(vertx, "localhost", 8080)
            .usePlaintext(true)
            .build();
        GreeterGrpc.GreeterVertxStub stub = GreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
        stub.sayHello(request, asyncResponse -> {
          if (asyncResponse.succeeded()) {
            System.out.println("Succeeded " + asyncResponse.result().getMessage());
          } else {
            asyncResponse.cause().printStackTrace();
          }
        });
      } else {
        asyncStart.cause().printStackTrace();
      }
    });
  }
}
