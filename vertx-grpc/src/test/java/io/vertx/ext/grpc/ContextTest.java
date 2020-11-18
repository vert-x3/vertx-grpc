package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.VertxStreamingGrpc;
import io.grpc.stub.MetadataUtils;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.BlockingServerInterceptor;
import io.vertx.grpc.ContextServerInterceptor;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class ContextTest extends GrpcTestBase {

  private volatile ManagedChannel channel;

  @Override
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  private static final Metadata.Key<String> SESSION_ID_METADATA_KEY = Metadata.Key.of("sessionId", ASCII_STRING_MARSHALLER);

  @Test(timeout = 10_000L)
  public void testSimple(TestContext should) {
    Async test = should.async();

    BindableService service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        should.assertTrue(Context.isOnEventLoopThread());
        should.assertNotNull(ContextServerInterceptor.get("sessionId"));
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    ServerInterceptor contextInterceptor = new ContextServerInterceptor() {
      @Override
      public void bind(Metadata metadata, ConcurrentMap<String, String> context) {
        context.put("sessionId", metadata.get(SESSION_ID_METADATA_KEY));
      }
    };

    startServer(ServerInterceptors.intercept(service, contextInterceptor))
      .onFailure(should::fail)
      .onSuccess(v -> {
        Metadata extraHeaders = new Metadata();
        extraHeaders.put(Metadata.Key.of("sessionId", Metadata.ASCII_STRING_MARSHALLER), UUID.randomUUID().toString());
        ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);

        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .intercept(clientInterceptor)
          .usePlaintext()
          .build();

        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();

        stub.sayHello(request).onComplete(should.asyncAssertSuccess(res -> {
          should.assertTrue(Context.isOnEventLoopThread());
          should.assertEquals("Hello Julien", res.getMessage());
          test.complete();
        }));
      });
  }

  @Test(timeout = 10_000L)
  public void testBlocking(TestContext should) {

    ServerInterceptor contextInterceptor = new ContextServerInterceptor() {
      @Override
      public void bind(Metadata metadata, ConcurrentMap<String, String> context) {
        context.put("sessionId", metadata.get(SESSION_ID_METADATA_KEY));
      }
    };

    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        // run on worker
        should.assertTrue(Context.isOnWorkerThread());
        System.out.println("sleep on " + Thread.currentThread());
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return h.startCall(call, m);
      }
    };
    BindableService service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        should.assertTrue(Context.isOnEventLoopThread());
        should.assertNotNull(ContextServerInterceptor.get("sessionId"));
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    final Async tests = should.async(2);

    startServer(ServerInterceptors.intercept(service, contextInterceptor, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .onFailure(should::fail)
      .onSuccess(v -> {
        Metadata extraHeaders = new Metadata();
        extraHeaders.put(Metadata.Key.of("sessionId", Metadata.ASCII_STRING_MARSHALLER), UUID.randomUUID().toString());
        ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);

        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .intercept(clientInterceptor)
          .usePlaintext()
          .build();

        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        Arrays.asList("Julien", "Paulo").forEach(name -> stub
          .sayHello(HelloRequest.newBuilder().setName(name).build())
          .onComplete(should.asyncAssertSuccess(res -> {
            should.assertEquals("Hello " + name, res.getMessage());
            tests.countDown();
          })));
      });
  }
}
