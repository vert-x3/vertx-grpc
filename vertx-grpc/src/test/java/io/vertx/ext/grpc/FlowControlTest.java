package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.examples.manualflowcontrol.HelloReply;
import io.grpc.examples.manualflowcontrol.HelloRequest;
import io.grpc.examples.manualflowcontrol.StreamingGreeterGrpc;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.StreamingGrpc;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.server.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class FlowControlTest extends GrpcTestBase {

  public static void main(String[] args) throws Exception {
    new Thread(() -> {
      try {
        runServer();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
    Thread.sleep(2000);
    runClient();
  }

  public static void runClient() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);

    // Create a channel and a stub
    ManagedChannel channel = ManagedChannelBuilder
      .forAddress("localhost", 8080)
      .usePlaintext()
      .build();
    StreamingGreeterGrpc.StreamingGreeterStub stub = StreamingGreeterGrpc.newStub(channel);

    // When using manual flow-control and back-pressure on the client, the ClientResponseObserver handles both
    // request and response streams.
    ClientResponseObserver<HelloRequest, HelloReply> clientResponseObserver =
      new ClientResponseObserver<HelloRequest, HelloReply>() {

        ClientCallStreamObserver<HelloRequest> requestStream;

        @Override
        public void beforeStart(final ClientCallStreamObserver<HelloRequest> requestStream) {
          this.requestStream = requestStream;
          // Set up manual flow control for the response stream. It feels backwards to configure the response
          // stream's flow control using the request stream's observer, but this is the way it is.
          requestStream.disableAutoRequestWithInitial(1);

          // Set up a back-pressure-aware producer for the request stream. The onReadyHandler will be invoked
          // when the consuming side has enough buffer space to receive more messages.
          //
          // Messages are serialized into a transport-specific transmit buffer. Depending on the size of this buffer,
          // MANY messages may be buffered, however, they haven't yet been sent to the server. The server must call
          // request() to pull a buffered message from the client.
          //
          // Note: the onReadyHandler's invocation is serialized on the same thread pool as the incoming
          // StreamObserver's onNext(), onError(), and onComplete() handlers. Blocking the onReadyHandler will prevent
          // additional messages from being processed by the incoming StreamObserver. The onReadyHandler must return
          // in a timely manner or else message processing throughput will suffer.
          requestStream.setOnReadyHandler(new Runnable() {
            // An iterator is used so we can pause and resume iteration of the request data.
            Iterator<String> iterator = names().iterator();

            @Override
            public void run() {
              // Start generating values from where we left off on a non-gRPC thread.
              System.out.println("Client start emitting");
              while (requestStream.isReady()) {
                if (iterator.hasNext()) {
                  // Send more messages if there are more messages to send.
                  String name = iterator.next();
                  System.out.println("--> " + name.length());
                  HelloRequest request = HelloRequest.newBuilder().setName(name).build();
                  requestStream.onNext(request);
                } else {
                  // Signal completion if there is nothing left to send.
                  requestStream.onCompleted();
                  System.out.println("Client completed");
                }
              }
              if (iterator.hasNext()) {
                System.out.println("Client stop emitting");
              }
            }
          });
        }

        @Override
        public void onNext(HelloReply value) {
          System.out.println("<-- " + value.getMessage().length());
          // Signal the sender to send one message.
          requestStream.request(1);
        }

        @Override
        public void onError(Throwable t) {
          t.printStackTrace();
          done.countDown();
        }

        @Override
        public void onCompleted() {
          System.out.println("All Done");
          done.countDown();
        }
      };

    // Note: clientResponseObserver is handling both request and response stream processing.
    stub.sayHelloStreaming(clientResponseObserver);

    done.await();

    channel.shutdown();
    channel.awaitTermination(1, TimeUnit.SECONDS);
  }

  public static void runServer() throws InterruptedException, IOException {
    // Service class implementation
    StreamingGreeterGrpc.StreamingGreeterImplBase svc = new StreamingGreeterGrpc.StreamingGreeterImplBase() {
      @Override
      public StreamObserver<HelloRequest> sayHelloStreaming(final StreamObserver<HelloReply> responseObserver) {
        // Set up manual flow control for the request stream. It feels backwards to configure the request
        // stream's flow control using the response stream's observer, but this is the way it is.
        final ServerCallStreamObserver<HelloReply> serverCallStreamObserver =
          (ServerCallStreamObserver<HelloReply>) responseObserver;
        serverCallStreamObserver.disableAutoRequest();

        // Set up a back-pressure-aware consumer for the request stream. The onReadyHandler will be invoked
        // when the consuming side has enough buffer space to receive more messages.
        //
        // Note: the onReadyHandler's invocation is serialized on the same thread pool as the incoming StreamObserver's
        // onNext(), onError(), and onComplete() handlers. Blocking the onReadyHandler will prevent additional messages
        // from being processed by the incoming StreamObserver. The onReadyHandler must return in a timely manner or
        // else message processing throughput will suffer.
        class OnReadyHandler implements Runnable {
          // Guard against spurious onReady() calls caused by a race between onNext() and onReady(). If the transport
          // toggles isReady() from false to true while onNext() is executing, but before onNext() checks isReady(),
          // request(1) would be called twice - once by onNext() and once by the onReady() scheduled during onNext()'s
          // execution.
          private boolean wasReady = false;

          @Override
          public void run() {
            if (serverCallStreamObserver.isReady() && !wasReady) {
              wasReady = true;
              System.out.println("Server ready");
              // Signal the request sender to send one message. This happens when isReady() turns true, signaling that
              // the receive buffer has enough free space to receive more messages. Calling request() serves to prime
              // the message pump.
              serverCallStreamObserver.request(1);
            }
          }
        }
        final OnReadyHandler onReadyHandler = new OnReadyHandler();
        serverCallStreamObserver.setOnReadyHandler(onReadyHandler);

        // Give gRPC a StreamObserver that can observe and process incoming requests.
        return new StreamObserver<HelloRequest>() {
          @Override
          public void onNext(HelloRequest request) {
            // Process the request and send a response or an error.
            try {
              // Accept and enqueue the request.
              String name = request.getName();
              System.out.println("--> " + name.length());

              // Simulate server "work"
              Thread.sleep(10);

              // Send a response.
              String message = "Hello " + name;
              System.out.println("<-- " + message.length());
              HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
              // responseObserver.onNext(reply);

              // Check the provided ServerCallStreamObserver to see if it is still ready to accept more messages.
              if (serverCallStreamObserver.isReady()) {
                // Signal the sender to send another request. As long as isReady() stays true, the server will keep
                // cycling through the loop of onNext() -> request(1)...onNext() -> request(1)... until the client runs
                // out of messages and ends the loop (via onCompleted()).
                //
                // If request() was called here with the argument of more than 1, the server might runs out of receive
                // buffer space, and isReady() will turn false. When the receive buffer has sufficiently drained,
                // isReady() will turn true, and the serverCallStreamObserver's onReadyHandler will be called to restart
                // the message pump.
                serverCallStreamObserver.request(1);
              } else {
                // If not, note that back-pressure has begun.
                onReadyHandler.wasReady = false;
              }
            } catch (Throwable throwable) {
              throwable.printStackTrace();
              responseObserver.onError(
                Status.UNKNOWN.withDescription("Error handling request").withCause(throwable).asException());
            }
          }

          @Override
          public void onError(Throwable t) {
            // End the response stream if the client presents an error.
            t.printStackTrace();
            responseObserver.onCompleted();
          }

          @Override
          public void onCompleted() {
            // Signal the end of work when the client ends the request stream.
            System.out.println("Server completed");
            responseObserver.onCompleted();
          }
        };
      }
    };

    final Server server = ServerBuilder
      .forPort(8080)
      .addService(svc)
      .build()
      .start();

    System.out.println("Listening on " + server.getPort());

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("Shutting down");
        try {
          server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
      }
    });
    server.awaitTermination();
  }

  private static String gen(int size) {
    StringBuilder sb = new StringBuilder(size);
    for (int i = 0;i < size;i++) {
      sb.append((int)'A' + (i % 26));
    }
    return sb.toString();
  }

  private static List<String> names() {
    String item = gen(1024 );
    int num = 1000;
    ArrayList<String> names = new ArrayList<>();
    for (int i = 0;i < num;i++) {
      names.add(item);
    }
    return names;
  }

  private static List<String> names2() {
    return Arrays.asList(
      "Sophia",
      "Jackson",
      "Emma",
      "Aiden",
      "Olivia",
      "Lucas",
      "Ava",
      "Liam",
      "Mia",
      "Noah",
      "Isabella",
      "Ethan",
      "Riley",
      "Mason",
      "Aria",
      "Caden",
      "Zoe",
      "Oliver",
      "Charlotte",
      "Elijah",
      "Lily",
      "Grayson",
      "Layla",
      "Jacob",
      "Amelia",
      "Michael",
      "Emily",
      "Benjamin",
      "Madelyn",
      "Carter",
      "Aubrey",
      "James",
      "Adalyn",
      "Jayden",
      "Madison",
      "Logan",
      "Chloe",
      "Alexander",
      "Harper",
      "Caleb",
      "Abigail",
      "Ryan",
      "Aaliyah",
      "Luke",
      "Avery",
      "Daniel",
      "Evelyn",
      "Jack",
      "Kaylee",
      "William",
      "Ella",
      "Owen",
      "Ellie",
      "Gabriel",
      "Scarlett",
      "Matthew",
      "Arianna",
      "Connor",
      "Hailey",
      "Jayce",
      "Nora",
      "Isaac",
      "Addison",
      "Sebastian",
      "Brooklyn",
      "Henry",
      "Hannah",
      "Muhammad",
      "Mila",
      "Cameron",
      "Leah",
      "Wyatt",
      "Elizabeth",
      "Dylan",
      "Sarah",
      "Nathan",
      "Eliana",
      "Nicholas",
      "Mackenzie",
      "Julian",
      "Peyton",
      "Eli",
      "Maria",
      "Levi",
      "Grace",
      "Isaiah",
      "Adeline",
      "Landon",
      "Elena",
      "David",
      "Anna",
      "Christian",
      "Victoria",
      "Andrew",
      "Camilla",
      "Brayden",
      "Lillian",
      "John",
      "Natalie",
      "Lincoln"
    );
  }

  @Test
  public void testServerStreaming(TestContext should) throws Exception {

    Async done = should.async();

    AtomicInteger count = new AtomicInteger();
    CompletableFuture<Integer> latch = new CompletableFuture<>();

    GrpcServer service = new GrpcServer();
    service.callHandler(StreamingGrpc.getSourceMethod(), callRequest -> {
      Promise<Void> p = Promise.promise();
      send(() -> Item.newBuilder().setValue("the-value-" + count.getAndIncrement()).build(), callRequest.response(), p);
      p.future().onComplete(should.asyncAssertSuccess(v2 -> {
        latch.complete(count.get());
        callRequest.response().end();
      }));
    });
    vertx.createHttpServer().requestHandler(service).listen(port, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSourceMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              callResponse.pause();
              AtomicInteger received = new AtomicInteger();
              callResponse.handler(item -> {
                should.assertTrue(latch.isDone());
                should.assertEquals("the-value-" + received.getAndIncrement(), item.getValue());
              });
              callResponse.endHandler(v2 -> {
                should.assertTrue(latch.isDone());
                should.assertEquals(received.get(), count.get());
                done.complete();
              });
              latch.whenComplete((v2, err) -> {
                callResponse.resume();
              });
            }));
            callRequest.end(Empty.getDefaultInstance());
          }));
      }));
  }

  @Test
  public void testClientStreaming(TestContext should) throws Exception {

    Async done = should.async();

    AtomicInteger count = new AtomicInteger();
    CompletableFuture<Integer> latch = new CompletableFuture<>();

    GrpcServer service = new GrpcServer();
    service.callHandler(StreamingGrpc.getSinkMethod(), call -> {
      call.pause();
      AtomicInteger received = new AtomicInteger();
      call.handler(item -> {
        should.assertTrue(latch.isDone());
        should.assertEquals("the-value-" + received.getAndIncrement(), item.getValue());
      });
      call.endHandler(v -> {
        should.assertTrue(latch.isDone());
        should.assertEquals(received.get(), count.get());
        call.response().end(Empty.getDefaultInstance());
      });
      latch.whenComplete((v, err) -> {
        call.resume();
      });
    });

    vertx.createHttpServer().requestHandler(service).listen(port, "localhost")
      .onComplete(should.asyncAssertSuccess(v -> {
        GrpcClient client = new GrpcClient(vertx);
        client.call(SocketAddress.inetSocketAddress(port, "localhost"), StreamingGrpc.getSinkMethod())
          .onComplete(should.asyncAssertSuccess(callRequest -> {
            callRequest.response().onComplete(should.asyncAssertSuccess(callResponse -> {
              callResponse.messageHandler(item -> {
                count.incrementAndGet();
              });
              callResponse.endHandler(v2 -> {
                done.complete();
              });
            }));
            Promise<Void> p = Promise.promise();
            send(() -> Item.newBuilder().setValue("the-value-" + count.getAndIncrement()).build(), callRequest, p);
            p.future().onComplete(should.asyncAssertSuccess(v2 -> {
              latch.complete(count.get());
              callRequest.end();
            }));
          }));
      }));
  }

  private <I> void send(Supplier<I> supplier, WriteStream<I> stream, Promise<Void> p) {
    while (!stream.writeQueueFull()) {
      I item = supplier.get();
      stream.write(item);
    }
    Vertx.currentContext().owner().setTimer(100, id -> {
      if (stream.writeQueueFull()) {
        p.complete();
      } else {
        send(supplier, stream, p);
      }
    });
  }

}
