package io.vertx.ext.grpc;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ProxyTest extends GrpcTestBase2 {

  @Test
  public void testUnary(TestContext should) {

    GrpcClient client = new GrpcClient(vertx);

    Future<HttpServer> server = vertx.createHttpServer().requestHandler(new GrpcServer().callHandler(GreeterGrpc.getSayHelloMethod(), call -> {
      call.handler(helloRequest -> {
        HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
        call.response().end(helloReply);
      });
    })).listen(8080, "localhost");

    Future<HttpServer> proxy = vertx.createHttpServer().requestHandler(new GrpcServer().requestHandler(clientReq -> {
      clientReq.pause();
      client.request(SocketAddress.inetSocketAddress(8080, "localhost")).onComplete(should.asyncAssertSuccess(proxyReq -> {
        proxyReq.response().onSuccess(resp -> {
          resp.handler(msg -> {
            clientReq.response().write(msg);
          });
          resp.endHandler(v -> {
            clientReq.response().end();
          });
        });
        proxyReq.fullMethodName(clientReq.fullMethodName());
        clientReq.messageHandler(proxyReq::write);
        clientReq.endHandler(v -> proxyReq.end());
        clientReq.resume();
      }));
    })).listen(8081, "localhost");

    Async test = should.async();
    server.flatMap(v -> proxy).onComplete(should.asyncAssertSuccess(v -> {
      client.call(SocketAddress.inetSocketAddress(8081, "localhost"), GreeterGrpc.getSayHelloMethod())
        .onComplete(should.asyncAssertSuccess(callRequest -> {
          callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
            AtomicInteger count = new AtomicInteger();
            callResponse.messageHandler(reply -> {
              should.assertEquals(1, count.incrementAndGet());
              should.assertEquals("Hello Julien", reply.getMessage());
            });
            callResponse.endHandler(v2 -> {
              should.assertEquals(1, count.get());
              test.complete();
            });
          }));
          callRequest.end(HelloRequest.newBuilder().setName("Julien").build());
        }));
    }));
  }
}
