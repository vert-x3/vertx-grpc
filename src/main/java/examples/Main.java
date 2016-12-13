package examples;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.grpc.StreamHelper;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    VertxServer server = VertxServerBuilder.forAddress(vertx, "localhost", 8080).addService(new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        System.out.println("Hello" + request.getName());
        responseObserver.onNext(HelloReply.newBuilder().setMessage(request.getName()).build());
        responseObserver.onCompleted();
      }
    }).build();
    server.start(asyncStart -> {
      if (asyncStart.succeeded()) {
        ManagedChannel channel = VertxChannelBuilder
            .forAddress(vertx, "localhost", 8080)
            .usePlaintext(true)
            .build();
        GreeterGrpc.GreeterStub blockingStub = GreeterGrpc.newStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
        blockingStub.sayHello(request, StreamHelper.future(asyncResponse -> {
          if (asyncResponse.succeeded()) {
            System.out.println("Succeeded " + asyncResponse.result().getMessage());
          } else {
            asyncResponse.cause().printStackTrace();
          }
        }));
      } else {
        asyncStart.cause().printStackTrace();
      }
    });
  }
}
