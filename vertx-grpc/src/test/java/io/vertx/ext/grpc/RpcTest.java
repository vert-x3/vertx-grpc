package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.VertxStreamingGrpc;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.*;
import org.junit.Test;

import io.grpc.examples.helloworld.*;
import io.vertx.core.streams.WriteStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class RpcTest extends GrpcTestBase {

  private volatile ManagedChannel channel;

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    if (channel != null) {
      channel.shutdown();
    }
  }

  @Test
  public void testSimple(TestContext ctx) {
    Async started = ctx.async();
    Context serverCtx = vertx.getOrCreateContext();
    serverCtx.runOnContext(v1 -> startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        ctx.assertEquals(serverCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());

        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    }, ctx.asyncAssertSuccess(v2 -> started.complete())));
    started.awaitSuccess(10000);
    Async async = ctx.async();
    Context clientCtx = vertx.getOrCreateContext();
    clientCtx.runOnContext(v -> {
      channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
       .usePlaintext()
       .build();
      VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
      HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
      stub.sayHello(request).onComplete(ctx.asyncAssertSuccess(res -> {
        ctx.assertEquals(clientCtx, Vertx.currentContext());
        ctx.assertTrue(Context.isOnEventLoopThread());
        ctx.assertEquals("Hello Julien", res.getMessage());
        async.complete();
      }));
    });
  }

  @Test
  public void testBlocking(TestContext ctx) throws Exception {
    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        // run on worker
        ctx.assertTrue(Context.isOnWorkerThread());
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

        ctx.assertTrue(Context.isOnEventLoopThread());
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };
    startServer(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)));
    Async async = ctx.async(2);
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext()
     .build();
    VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
    Arrays.asList("Julien", "Paulo").forEach(name -> {
      stub
        .sayHello(HelloRequest.newBuilder().setName(name).build())
        .onComplete(ctx.asyncAssertSuccess(res -> {
        ctx.assertEquals("Hello " + name, res.getMessage());
        async.countDown();
      }));
    });
    async.awaitSuccess(10000);
  }

  @Test
  public void testBlockingException(TestContext ctx) {
    Metadata.Key<String> mdKey = Metadata.Key.of("mdkey", Metadata.ASCII_STRING_MARSHALLER);
    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        Metadata md = new Metadata();
        md.put(mdKey, "mdvalue");
        throw new StatusRuntimeException(Status.ABORTED, md);
      }
    };
    BindableService service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };
    Async started = ctx.async();
    server = VertxServerBuilder.forPort(vertx, port)
     .addService(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
     .build()
     .start(ar -> {
       if (ar.succeeded()) {
         started.complete();
       } else {
         ctx.fail(ar.cause());
       }
     });
    started.awaitSuccess(10000);
    Async async = ctx.async();
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext()
     .build();
    VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
    stub.sayHello(HelloRequest.newBuilder().setName("Julien").build()).onComplete(ctx.asyncAssertFailure(err -> {
      ctx.assertTrue(err instanceof StatusRuntimeException);
      StatusRuntimeException sre = (StatusRuntimeException) err;
      ctx.assertEquals(Status.ABORTED, sre.getStatus());
      ctx.assertEquals("mdvalue", sre.getTrailers().get(mdKey));
      async.countDown();
    }));
    async.awaitSuccess(10000);
  }

  @Test
  public void testStreamSource(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, WriteStream<Item> response) {
        new IterableReadStream<>(cnt -> Item.newBuilder().setValue("the-value-" + cnt).build(), numItems).pipeTo(response);
      }
    });
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext()
     .build();
    VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    stub.source(Empty.newBuilder().build())
     .endHandler(v -> {
       List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
       ctx.assertEquals(expected, items);
       done.complete();
     })
     .exceptionHandler(ctx::fail)
     .handler(item -> items.add(item.getValue()));
  }

  @Test
  public void testStreamSink(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public Future<Empty> sink(ReadStream<Item> request) {
        List<String> items = new ArrayList<>();
        Promise<Empty> promise = Promise.promise();

        request
         .endHandler(v -> {
           List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
           ctx.assertEquals(expected, items);
           done.complete();
           promise.complete(Empty.getDefaultInstance());
         })
         .exceptionHandler(ctx::fail)
         .handler(item -> items.add(item.getValue()));
        return promise.future();
      }
    });
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext()
     .build();
    VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
    AtomicInteger count = new AtomicInteger(numItems);

    Handler<WriteStream<Item>> h = ws -> {
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          ws.end();
        }
      });

    };

    stub.sink(h)
     .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void testStreamPipe(TestContext ctx) throws Exception {
    int numItems = 128;
    Async done = ctx.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void pipe(ReadStream<Item> request, WriteStream<Item> response) {
        request.pipeTo(response);
      }
    });
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
     .usePlaintext()
     .build();
    VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
    final List<String> items = new ArrayList<>();
    AtomicInteger count = new AtomicInteger(numItems);

    Handler<WriteStream<Item>> h = ws -> {
      vertx.setPeriodic(10, id -> {
        int val = count.decrementAndGet();
        if (val >= 0) {
          ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
        } else {
          vertx.cancelTimer(id);
          ws.end();
        }
      });
    };

    stub.pipe(h)
     .endHandler(v -> {
       List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
       ctx.assertEquals(expected, items);
       done.complete();
     })
     .exceptionHandler(ctx::fail)
     .handler(item -> items.add(item.getValue()));

  }

  @Test
  public void testRandomPort(TestContext ctx) throws Exception {
    Async started = ctx.async();
    port = 0;
    startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }

    }, ar -> {
      if (ar.succeeded()) {
        started.complete();
      } else {
        ctx.fail(ar.cause());
      }
    });

    started.awaitSuccess(10000);

    ctx.assertTrue(server.getPort() > 0);
    ctx.assertTrue(server.getPort() < 65536);
  }

  @Test
  public void testClientCompression(TestContext ctx) throws Exception {
    Metadata.Key<String> encodingKey = Metadata.Key.of("grpc-encoding", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> acceptEncodingKey = Metadata.Key.of("grpc-accept-encoding", Metadata.ASCII_STRING_MARSHALLER);
    AtomicReference<String> clientEncoding = new AtomicReference<>();
    AtomicReference<String> clientAcceptEncoding = new AtomicReference<>();
    AtomicReference<String> serverEncoding = new AtomicReference<>();

    ServerInterceptor blockingInterceptor = new ServerInterceptor() {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(ServerCall<Q, A> call, Metadata m, ServerCallHandler<Q, A> h) {
        clientEncoding.set(m.get(encodingKey));
        clientAcceptEncoding.set(m.get(acceptEncodingKey));
        return h.startCall(call, m);
      }
    };

    VertxGreeterGrpc.GreeterVertxImplBase service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    }.withCompression("gzip");
    startServer(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)));

    Async async = ctx.async();
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .usePlaintext()
      .build();
    VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();
    stub
      .withInterceptors(new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
          ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
          return new ForwardingClientCall<ReqT, RespT>() {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              super.start(new ForwardingClientCallListener<RespT>() {
                @Override
                public void onHeaders(Metadata headers) {
                  serverEncoding.set(headers.get(encodingKey));
                  super.onHeaders(headers);
                }
                @Override
                protected Listener<RespT> delegate() {
                  return responseListener;
                }
              }, headers);
            }
            @Override
            protected ClientCall<ReqT, RespT> delegate() {
              return call;
            }
          };
        }
      })
      .withCompression("gzip")
      .sayHello(request)
      .onComplete(ctx.asyncAssertSuccess(res -> {
      ctx.assertEquals("Hello Julien", res.getMessage());
      async.complete();
    }));

    async.awaitSuccess(20000);
    assertEquals("gzip", clientEncoding.get());
    assertEquals("gzip", clientAcceptEncoding.get());
    assertEquals("gzip", serverEncoding.get());
  }
}
