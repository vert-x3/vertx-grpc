package examples;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.*;
import io.vertx.core.Context;
import io.vertx.core.net.JksOptions;
import io.vertx.docgen.Source;
import io.vertx.grpc.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Source
public class Examples {

  public void simpleServer(Vertx vertx) throws Exception {
    // The rcp service
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(
        HelloRequest request,
        StreamObserver<HelloReply> responseObserver) {

        responseObserver.onNext(
          HelloReply.newBuilder()
            .setMessage(request.getName())
            .build());
        responseObserver.onCompleted();
      }
    };
  }

  public void startServer(BindableService service, Vertx vertx) throws Exception {
    // Create the server
    VertxServer rpcServer = VertxServerBuilder
      .forAddress(vertx, "my.host", 8080)
      .addService(service)
      .build();

    // Start is asynchronous
    rpcServer.start();
  }

  public void vertxSimpleServer(Vertx vertx) throws Exception {
    // The rcp service
    VertxGreeterGrpc.GreeterVertxImplBase service =
      new VertxGreeterGrpc.GreeterVertxImplBase() {
        @Override
        public Future<HelloReply> sayHello(HelloRequest request) {
          return Future.succeededFuture(
            HelloReply.newBuilder()
              .setMessage(request.getName())
              .build());
        }
      };
  }

  public void serverWithCompression(Vertx vertx) {
    // The rcp service
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(
        HelloRequest request,
        StreamObserver<HelloReply> responseObserver) {

        ((ServerCallStreamObserver) responseObserver)
          .setCompression("gzip");

        responseObserver.onNext(
          HelloReply.newBuilder()
            .setMessage(request.getName())
            .build());

        responseObserver.onCompleted();
      }
    };
  }

  public void vertxServerWithCompression() {
    // The rcp service
    VertxGreeterGrpc.GreeterVertxImplBase service =
      new VertxGreeterGrpc.GreeterVertxImplBase() {
        @Override
        public Future<HelloReply> sayHello(HelloRequest request) {
          return Future.succeededFuture(
            HelloReply.newBuilder()
              .setMessage(request.getName())
              .build());
        }
      }
        .withCompression("gzip");
  }

  public void connectClient(Vertx vertx) {
    // Create the channel
    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "localhost", 8080)
      .usePlaintext()
      .build();

    // Get a stub to use for interacting with the remote service
    GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
  }

  public void simpleClient(GreeterGrpc.GreeterStub stub) {
    // Make a request
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();

    // Call the remote service
    stub.sayHello(request, new StreamObserver<HelloReply>() {
      private HelloReply helloReply;

      @Override
      public void onNext(HelloReply helloReply) {
        this.helloReply = helloReply;
      }

      @Override
      public void onError(Throwable throwable) {
        System.out.println("Coult not reach server " + throwable.getMessage());
      }

      @Override
      public void onCompleted() {
        System.out.println("Got the server response: " + helloReply.getMessage());
      }
    });
  }

  public void vertxSimpleClient(VertxGreeterGrpc.GreeterVertxStub stub) {
    // Make a request
    HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();

    // Call the remote service
    Future<HelloReply> future = stub.sayHello(request);

    // Listen to completion events
    future
      .onSuccess(helloReply -> System.out.println("Got the server response: " + helloReply.getMessage())).onFailure(err -> System.out.println("Coult not reach server " + err));
  }

  public void clientWithCompression(ManagedChannel channel) {
    // Get a stub to use for interacting with the
    // remote service with message compression
    GreeterGrpc.GreeterStub stub = GreeterGrpc
      .newStub(channel)
      .withCompression("gzip");
  }

  public void sslServer(Vertx vertx) {
    VertxServerBuilder builder = VertxServerBuilder.forPort(vertx, 8080)
      .useSsl(options -> options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("server-keystore.jks")
          .setPassword("secret")));
  }

  public void serverScaling(Vertx vertx) {

    vertx.deployVerticle(

      // Verticle supplier - should be called 4 times
      () -> new AbstractVerticle() {

        BindableService service = new GreeterGrpc.GreeterImplBase() {
          @Override
          public void sayHello(
            HelloRequest request,
            StreamObserver<HelloReply> responseObserver) {

            responseObserver.onNext(
              HelloReply.newBuilder()
                .setMessage(request.getName())
                .build());

            responseObserver.onCompleted();
          }
        };

        @Override
        public void start() throws Exception {
          VertxServerBuilder
            .forAddress(vertx, "my.host", 8080)
            .addService(service)
            .build()
            .start();
        }
      },

      // Deploy 4 instances, i.e the service is scaled on 4 event-loops
      new DeploymentOptions()
        .setInstances(4));
  }

  public void sslClient(Vertx vertx) {
    ManagedChannel channel = VertxChannelBuilder.
      forAddress(vertx, "localhost", 8080)
      .useSsl(options -> options
        .setSsl(true)
        .setUseAlpn(true)
        .setKeyCertOptions(new JksOptions()
          .setPath("client-truststore.jks")
          .setPassword("secret")))
      .build();
  }

  public void blockingInterceptor() {
    class MyInterceptor implements ServerInterceptor {
      @Override
      public <Q, A> ServerCall.Listener<Q> interceptCall(
        ServerCall<Q, A> call, Metadata headers, ServerCallHandler<Q, A> next) {
        // do something hard and update the metadata, for example
        return next.startCall(call, headers);
      }
    }
    MyInterceptor myInterceptor = new MyInterceptor();
  }

  public <MyInterceptor extends ServerInterceptor> void nonblockingInterceptorUsage(
    MyInterceptor myInterceptor, Vertx vertx, BindableService service) {
    VertxServer rpcServer = VertxServerBuilder
      .forAddress(vertx, "my.host", 8080)
      .addService(ServerInterceptors.intercept(service, myInterceptor))
      .build();
  }

  public <MyInterceptor extends ServerInterceptor> void blockingInterceptorUsage(
    MyInterceptor myInterceptor,
    Vertx vertx,
    BindableService service) throws Exception {

    // wrap interceptor to execute on worker thread instead of event loop
    ServerInterceptor wrapped =
      BlockingServerInterceptor.wrap(vertx, myInterceptor);

    // Create the server
    VertxServer rpcServer = VertxServerBuilder
      .forAddress(vertx, "my.host", 8080)
      .addService(ServerInterceptors.intercept(service, wrapped))
      .build();

    // Start it
    rpcServer.start();
  }

  public void nativeTransport() {
    // Use native transports
    Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
  }

  public void clientSessionInterceptorUsage(ManagedChannel channel, Vertx vertx, String theSessionId, int port) {
    Metadata extraHeaders = new Metadata();
    extraHeaders.put(
      Metadata.Key.of("sessionId", Metadata.ASCII_STRING_MARSHALLER), theSessionId);

    ClientInterceptor clientInterceptor = MetadataUtils
      .newAttachHeadersInterceptor(extraHeaders);

    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port)
      .intercept(clientInterceptor)
      .build();
  }

  public void serverSessionInterceptorUsage(Vertx vertx, Metadata.Key<String> SESSION_ID_METADATA_KEY) {
    BindableService service = new VertxGreeterGrpc.GreeterVertxImplBase() {
      @Override
      public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(
          HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
      }
    };

    ServerInterceptor contextInterceptor = new ContextServerInterceptor() {
      @Override
      public void bind(Metadata metadata) {
        put("sessionId", metadata.get(SESSION_ID_METADATA_KEY));
      }
    };

    // Create the server
    VertxServer rpcServer = VertxServerBuilder
      .forAddress(vertx, "my.host", 8080)
      .addService(ServerInterceptors.intercept(service, contextInterceptor))
      .build();
  }
}
