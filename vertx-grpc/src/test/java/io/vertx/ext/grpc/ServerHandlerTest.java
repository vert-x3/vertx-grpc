package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.vertx.core.Context;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.server.GrpcService;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ServerHandlerTest extends GrpcTestBase {

  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test
  public void testUnary(TestContext should) throws Exception {

    Async test = should.async();

    VertxGreeterGrpc.GreeterVertxImplBase greeterVertxImplBase = new VertxGreeterGrpc.GreeterVertxImplBase() {
    };
    ServerServiceDefinition serviceDef = greeterVertxImplBase.bindService();

    GrpcService service = new GrpcService().serviceDefinition(serviceDef);
    service.requestHandler(request -> {
      request.messageHandler(msg -> {
        ByteArrayInputStream in = new ByteArrayInputStream(msg.data().getBytes());
        HelloRequest helloRequest = (HelloRequest) request.methodDefinition().getMethodDescriptor().parseRequest(in);
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        request.response().write(helloReply);
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(8080, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();
        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
        stub.sayHello(request).onComplete(should.asyncAssertSuccess(res -> {
          should.assertTrue(Context.isOnEventLoopThread());
          should.assertEquals("Hello Julien", res.getMessage());
          test.complete();
        }));
      }));
  }
}
