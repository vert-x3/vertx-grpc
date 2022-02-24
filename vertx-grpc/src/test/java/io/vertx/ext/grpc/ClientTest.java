package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.server.GrpcClient;
import io.vertx.grpc.server.GrpcMessage;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ClientTest extends GrpcTestBase {

  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) {

    Async test = should.async();

    startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        System.out.println("REQUEST");
        should.assertTrue(Context.isOnEventLoopThread());
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.request(SocketAddress.inetSocketAddress(port, "localhost"))
          .onSuccess(request -> {
            HelloRequest item = HelloRequest.newBuilder().setName("Julien").build();
            InputStream stream = GreeterGrpc.getSayHelloMethod().streamRequest(item);
            Buffer encoded = Buffer.buffer();
            byte[] tmp = new byte[256];
            int i;
            try {
              while ((i = stream.read(tmp)) != -1) {
                encoded.appendBytes(tmp, 0, i);
              }
            } catch (IOException e) {
              throw new VertxException(e);
            }
            request.fullMethodName(GreeterGrpc.getSayHelloMethod().getFullMethodName());
            request.response().onSuccess(resp -> {
              System.out.println("GOT RESP ");
              resp.handler(msg -> {
                System.out.println("got msg " + msg);
              });
              resp.endHandler(v2 -> {
                System.out.println("END");
              });
            });
            request.end(new GrpcMessage(encoded));
          });

//        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
//          .usePlaintext()
//          .build();
//
//        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
//        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
//
//        stub.sayHello(request).onComplete(should.asyncAssertSuccess(res -> {
//          should.assertTrue(Context.isOnEventLoopThread());
//          should.assertEquals("Hello Julien", res.getMessage());
//          test.complete();
//        }));
      });
  }
}
