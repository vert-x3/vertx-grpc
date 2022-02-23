package io.vertx.ext.grpc;

import io.grpc.*;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpc;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.VertxStreamingGrpc;
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
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Test;

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
  public void tearDown(TestContext should) {
    if (channel != null) {
      channel.shutdown();
    }
    super.tearDown(should);
  }

  @Test(timeout = 10_000L)
  public void testSimple(TestContext should) {
    Async test = should.async();

    startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        should.assertTrue(Context.isOnEventLoopThread());
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
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
      });
  }

  @Test(timeout = 10_000L)
  public void testBlocking(TestContext should) {
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
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    final Async tests = should.async(2);

    startServer(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .onFailure(should::fail)
      .onSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
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

  @Test(timeout = 10_000L)
  public void testBlockingException(TestContext should) {
    final Async test = should.async();

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
    server = VertxServerBuilder.forPort(vertx, port)
      .addService(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .build()
      .start(ar -> {
        if (ar.failed()) {
          should.fail(ar.cause());
          return;
        }

        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxGreeterGrpc.GreeterVertxStub stub = VertxGreeterGrpc.newVertxStub(channel);
        stub.sayHello(HelloRequest.newBuilder().setName("Julien").build()).onComplete(should.asyncAssertFailure(err -> {
          should.assertTrue(err instanceof StatusRuntimeException);
          StatusRuntimeException sre = (StatusRuntimeException) err;
          should.assertEquals(Status.ABORTED, sre.getStatus());
          should.assertEquals("mdvalue", sre.getTrailers().get(mdKey));
          test.complete();
        }));
      });
  }

  @Test(timeout = 10_000L)
  public void testStreamSource(TestContext should) {
    int numItems = 128;
    final Async test = should.async();

    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void source(Empty request, WriteStream<Item> response) {
        new IterableReadStream<>(cnt -> Item.newBuilder().setValue("the-value-" + cnt).build(), numItems).pipeTo(response);
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        final List<String> items = new ArrayList<>();
        stub.source(Empty.newBuilder().build())
          .endHandler(v1 -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            test.complete();
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
      });
  }

  @Test(timeout = 10_000L)
  public void testStreamSink(TestContext should) {
    int numItems = 128;
    Async done = should.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public Future<Empty> sink(ReadStream<Item> request) {
        List<String> items = new ArrayList<>();
        Promise<Empty> promise = Promise.promise();
        request
          .endHandler(v -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            done.complete();
            promise.complete(Empty.getDefaultInstance());
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
        return promise.future();
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();

        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        AtomicInteger count = new AtomicInteger(numItems);

        Handler<WriteStream<Item>> h = ws -> vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            ws.end();
          }
        });

        stub.sink(h)
          .onComplete(should.asyncAssertSuccess());
      });
  }

  @Test(timeout = 10_000L)
  public void testStreamPipe(TestContext should) {
    int numItems = 128;
    Async done = should.async();
    startServer(new VertxStreamingGrpc.StreamingVertxImplBase() {
      @Override
      public void pipe(ReadStream<Item> request, WriteStream<Item> response) {
        request.pipeTo(response);
      }
    })
      .onFailure(should::fail)
      .onSuccess(v -> {
        channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
          .usePlaintext()
          .build();
        VertxStreamingGrpc.StreamingVertxStub stub = VertxStreamingGrpc.newVertxStub(channel);
        final List<String> items = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(numItems);

        Handler<WriteStream<Item>> h = ws -> vertx.setPeriodic(10, id -> {
          int val = count.decrementAndGet();
          if (val >= 0) {
            ws.write(Item.newBuilder().setValue("the-value-" + (numItems - val - 1)).build());
          } else {
            vertx.cancelTimer(id);
            ws.end();
          }
        });

        stub.pipe(h)
          .endHandler(v1 -> {
            List<String> expected = IntStream.rangeClosed(0, numItems - 1).mapToObj(val -> "the-value-" + val).collect(Collectors.toList());
            should.assertEquals(expected, items);
            done.complete();
          })
          .exceptionHandler(should::fail)
          .handler(item -> items.add(item.getValue()));
      });
  }

  @Test(timeout = 10_000L)
  public void testRandomPort(TestContext should) {
    Async test = should.async();
    port = 0;
    startServer(new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }

    }).onFailure(should::fail)
      .onSuccess(v -> {
        should.assertTrue(server.getPort() > 0);
        should.assertTrue(server.getPort() < 65536);
        test.complete();
      });
  }

  @Test(timeout = 10_000L)
  public void testClientCompression(TestContext should) {
    final Async test = should.async();

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

    startServer(ServerInterceptors.intercept(service, BlockingServerInterceptor.wrap(vertx, blockingInterceptor)))
      .onFailure(should::fail)
      .onSuccess(v -> {
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
          .onComplete(should.asyncAssertSuccess(res -> {
            should.assertEquals("Hello Julien", res.getMessage());
            assertEquals("gzip", clientEncoding.get());
            assertEquals("gzip", clientAcceptEncoding.get());
            assertEquals("gzip", serverEncoding.get());
            test.complete();
          }));
      });
  }
}
