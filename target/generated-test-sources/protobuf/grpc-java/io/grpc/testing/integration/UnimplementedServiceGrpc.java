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
 * A simple service NOT implemented at servers so clients can test for
 * that case.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.1)",
    comments = "Source: test.proto")
public class UnimplementedServiceGrpc {

  private UnimplementedServiceGrpc() {}

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

  public static final String SERVICE_NAME = "grpc.testing.UnimplementedService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.google.protobuf.EmptyProtos.Empty,
      com.google.protobuf.EmptyProtos.Empty> METHOD_UNIMPLEMENTED_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.UnimplementedService", "UnimplementedCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UnimplementedServiceStub newStub(io.grpc.Channel channel) {
    return new UnimplementedServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UnimplementedServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new UnimplementedServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static UnimplementedServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new UnimplementedServiceFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static UnimplementedServiceVertxStub newVertxStub(io.grpc.Channel channel) {
    return new UnimplementedServiceVertxStub(channel);
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static abstract class UnimplementedServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_UNIMPLEMENTED_CALL, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_UNIMPLEMENTED_CALL,
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_UNIMPLEMENTED_CALL)))
          .build();
    }
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static final class UnimplementedServiceStub extends io.grpc.stub.AbstractStub<UnimplementedServiceStub> {
    private UnimplementedServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UnimplementedServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UnimplementedServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UnimplementedServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UNIMPLEMENTED_CALL, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static final class UnimplementedServiceBlockingStub extends io.grpc.stub.AbstractStub<UnimplementedServiceBlockingStub> {
    private UnimplementedServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UnimplementedServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UnimplementedServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UnimplementedServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public com.google.protobuf.EmptyProtos.Empty unimplementedCall(com.google.protobuf.EmptyProtos.Empty request) {
      return blockingUnaryCall(
          getChannel(), METHOD_UNIMPLEMENTED_CALL, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static final class UnimplementedServiceFutureStub extends io.grpc.stub.AbstractStub<UnimplementedServiceFutureStub> {
    private UnimplementedServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UnimplementedServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UnimplementedServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UnimplementedServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.EmptyProtos.Empty> unimplementedCall(
        com.google.protobuf.EmptyProtos.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_UNIMPLEMENTED_CALL, getCallOptions()), request);
    }
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static abstract class UnimplementedServiceVertxImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty> response) {
      asyncUnimplementedUnaryCall(METHOD_UNIMPLEMENTED_CALL, UnimplementedServiceGrpc.toObserver(response.completer()));
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_UNIMPLEMENTED_CALL,
            asyncUnaryCall(
              new VertxMethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_UNIMPLEMENTED_CALL)))
          .build();
    }
  }

  /**
   * <pre>
   * A simple service NOT implemented at servers so clients can test for
   * that case.
   * </pre>
   */
  public static final class UnimplementedServiceVertxStub extends io.grpc.stub.AbstractStub<UnimplementedServiceVertxStub> {
    private UnimplementedServiceVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UnimplementedServiceVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UnimplementedServiceVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UnimplementedServiceVertxStub(channel, callOptions);
    }

    /**
     * <pre>
     * A call that no server should implement
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<com.google.protobuf.EmptyProtos.Empty>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UNIMPLEMENTED_CALL, getCallOptions()), request, UnimplementedServiceGrpc.toObserver(response));
    }
  }

  private static final int METHODID_UNIMPLEMENTED_CALL = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final UnimplementedServiceImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(UnimplementedServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UNIMPLEMENTED_CALL:
          serviceImpl.unimplementedCall((com.google.protobuf.EmptyProtos.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver);
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
    private final UnimplementedServiceVertxImplBase serviceImpl;
    private final int methodId;

    public VertxMethodHandlers(UnimplementedServiceVertxImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UNIMPLEMENTED_CALL:
          serviceImpl.unimplementedCall((com.google.protobuf.EmptyProtos.Empty) request,
              (io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty>) io.vertx.core.Future.<com.google.protobuf.EmptyProtos.Empty>future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver).onNext(ar.result());
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

  private static final class UnimplementedServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.testing.integration.Test.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UnimplementedServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UnimplementedServiceDescriptorSupplier())
              .addMethod(METHOD_UNIMPLEMENTED_CALL)
              .build();
        }
      }
    }
    return result;
  }
}
