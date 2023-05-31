package io.grpc.testing.integration;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 * <pre>
 * A service used to control reconnect server.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.1)",
    comments = "Source: test.proto")
public class ReconnectServiceGrpc {

  private ReconnectServiceGrpc() {}

  private static <T> io.grpc.stub.StreamObserver<T> toObserver(final io.vertx.core.Handler<io.vertx.core.AsyncResult<T>> handler) {
    return new io.grpc.stub.StreamObserver<T>() {
      private boolean resolved = false;
      @Override
      public void onNext(T value) {
        if (resolved) {
          throw new IllegalStateException("Already Resolved");
        }
        resolved = true;
        handler.handle(io.vertx.core.Future.succeededFuture(value));
      }

      @Override
      public void onError(Throwable t) {
        if (resolved) {
          throw new IllegalStateException("Already Resolved");
        }
        resolved = true;
        handler.handle(io.vertx.core.Future.failedFuture(t));
      }

      @Override
      public void onCompleted() {
        if (resolved) {
          return;
        }
        resolved = true;
        handler.handle(io.vertx.core.Future.succeededFuture());
      }
    };
  }

  public static final String SERVICE_NAME = "grpc.testing.ReconnectService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.google.protobuf.EmptyProtos.Empty,
      com.google.protobuf.EmptyProtos.Empty> METHOD_START =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.ReconnectService", "Start"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.google.protobuf.EmptyProtos.Empty,
      io.grpc.testing.integration.Messages.ReconnectInfo> METHOD_STOP =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.ReconnectService", "Stop"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.ReconnectInfo.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ReconnectServiceStub newStub(io.grpc.Channel channel) {
    return new ReconnectServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ReconnectServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ReconnectServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ReconnectServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ReconnectServiceFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static ReconnectServiceVertxStub newVertxStub(io.grpc.Channel channel) {
    return new ReconnectServiceVertxStub(channel);
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static abstract class ReconnectServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void start(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_START, responseObserver);
    }

    /**
     */
    public void stop(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.ReconnectInfo> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_STOP, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_START,
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_START)))
          .addMethod(
            METHOD_STOP,
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                io.grpc.testing.integration.Messages.ReconnectInfo>(
                  this, METHODID_STOP)))
          .build();
    }
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static final class ReconnectServiceStub extends io.grpc.stub.AbstractStub<ReconnectServiceStub> {
    private ReconnectServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ReconnectServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReconnectServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ReconnectServiceStub(channel, callOptions);
    }

    /**
     */
    public void start(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_START, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void stop(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.ReconnectInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_STOP, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static final class ReconnectServiceBlockingStub extends io.grpc.stub.AbstractStub<ReconnectServiceBlockingStub> {
    private ReconnectServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ReconnectServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReconnectServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ReconnectServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.EmptyProtos.Empty start(com.google.protobuf.EmptyProtos.Empty request) {
      return blockingUnaryCall(
          getChannel(), METHOD_START, getCallOptions(), request);
    }

    /**
     */
    public io.grpc.testing.integration.Messages.ReconnectInfo stop(com.google.protobuf.EmptyProtos.Empty request) {
      return blockingUnaryCall(
          getChannel(), METHOD_STOP, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static final class ReconnectServiceFutureStub extends io.grpc.stub.AbstractStub<ReconnectServiceFutureStub> {
    private ReconnectServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ReconnectServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReconnectServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ReconnectServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.EmptyProtos.Empty> start(
        com.google.protobuf.EmptyProtos.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_START, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.testing.integration.Messages.ReconnectInfo> stop(
        com.google.protobuf.EmptyProtos.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_STOP, getCallOptions()), request);
    }
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static abstract class ReconnectServiceVertxImplBase implements io.grpc.BindableService {

    /**
     */
    public void start(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty> response) {
      asyncUnimplementedUnaryCall(METHOD_START, ReconnectServiceGrpc.toObserver(response.completer()));
    }

    /**
     */
    public void stop(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Future<io.grpc.testing.integration.Messages.ReconnectInfo> response) {
      asyncUnimplementedUnaryCall(METHOD_STOP, ReconnectServiceGrpc.toObserver(response.completer()));
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_START,
            asyncUnaryCall(
              new VertxMethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_START)))
          .addMethod(
            METHOD_STOP,
            asyncUnaryCall(
              new VertxMethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                io.grpc.testing.integration.Messages.ReconnectInfo>(
                  this, METHODID_STOP)))
          .build();
    }
  }

  /**
   * <pre>
   * A service used to control reconnect server.
   * </pre>
   */
  public static final class ReconnectServiceVertxStub extends io.grpc.stub.AbstractStub<ReconnectServiceVertxStub> {
    private ReconnectServiceVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ReconnectServiceVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReconnectServiceVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ReconnectServiceVertxStub(channel, callOptions);
    }

    /**
     */
    public void start(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<com.google.protobuf.EmptyProtos.Empty>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_START, getCallOptions()), request, ReconnectServiceGrpc.toObserver(response));
    }

    /**
     */
    public void stop(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<io.grpc.testing.integration.Messages.ReconnectInfo>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_STOP, getCallOptions()), request, ReconnectServiceGrpc.toObserver(response));
    }
  }

  private static final int METHODID_START = 0;
  private static final int METHODID_STOP = 1;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ReconnectServiceImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(ReconnectServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_START:
          serviceImpl.start((com.google.protobuf.EmptyProtos.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver);
          break;
        case METHODID_STOP:
          serviceImpl.stop((com.google.protobuf.EmptyProtos.Empty) request,
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.ReconnectInfo>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static class VertxMethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ReconnectServiceVertxImplBase serviceImpl;
    private final int methodId;

    public VertxMethodHandlers(ReconnectServiceVertxImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_START:
          serviceImpl.start((com.google.protobuf.EmptyProtos.Empty) request,
              (io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty>) io.vertx.core.Future.<com.google.protobuf.EmptyProtos.Empty>future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver).onNext(ar.result());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(ar.cause());
                }
              }));
          break;
        case METHODID_STOP:
          serviceImpl.stop((com.google.protobuf.EmptyProtos.Empty) request,
              (io.vertx.core.Future<io.grpc.testing.integration.Messages.ReconnectInfo>) io.vertx.core.Future.<io.grpc.testing.integration.Messages.ReconnectInfo>future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.ReconnectInfo>) responseObserver).onNext(ar.result());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(ar.cause());
                }
              }));
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class ReconnectServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.testing.integration.Test.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ReconnectServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ReconnectServiceDescriptorSupplier())
              .addMethod(METHOD_START)
              .addMethod(METHOD_STOP)
              .build();
        }
      }
    }
    return result;
  }
}
