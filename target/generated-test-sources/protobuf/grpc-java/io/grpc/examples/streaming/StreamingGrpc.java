package io.grpc.examples.streaming;

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
 * Interface exported by the server.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.1)",
    comments = "Source: streaming.proto")
public class StreamingGrpc {

  private StreamingGrpc() {}

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

  public static final String SERVICE_NAME = "streaming.Streaming";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.examples.streaming.Empty,
      io.grpc.examples.streaming.Item> METHOD_SOURCE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "streaming.Streaming", "Source"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Item.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.examples.streaming.Item,
      io.grpc.examples.streaming.Empty> METHOD_SINK =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
          generateFullMethodName(
              "streaming.Streaming", "Sink"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Item.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Empty.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.examples.streaming.Item,
      io.grpc.examples.streaming.Item> METHOD_PIPE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "streaming.Streaming", "Pipe"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Item.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.streaming.Item.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StreamingStub newStub(io.grpc.Channel channel) {
    return new StreamingStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StreamingBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new StreamingBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static StreamingFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new StreamingFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static StreamingVertxStub newVertxStub(io.grpc.Channel channel) {
    return new StreamingVertxStub(channel);
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static abstract class StreamingImplBase implements io.grpc.BindableService {

    /**
     */
    public void source(io.grpc.examples.streaming.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SOURCE, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> sink(
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Empty> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_SINK, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> pipe(
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_PIPE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SOURCE,
            asyncServerStreamingCall(
              new MethodHandlers<
                io.grpc.examples.streaming.Empty,
                io.grpc.examples.streaming.Item>(
                  this, METHODID_SOURCE)))
          .addMethod(
            METHOD_SINK,
            asyncClientStreamingCall(
              new MethodHandlers<
                io.grpc.examples.streaming.Item,
                io.grpc.examples.streaming.Empty>(
                  this, METHODID_SINK)))
          .addMethod(
            METHOD_PIPE,
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.grpc.examples.streaming.Item,
                io.grpc.examples.streaming.Item>(
                  this, METHODID_PIPE)))
          .build();
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class StreamingStub extends io.grpc.stub.AbstractStub<StreamingStub> {
    private StreamingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private StreamingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new StreamingStub(channel, callOptions);
    }

    /**
     */
    public void source(io.grpc.examples.streaming.Empty request,
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_SOURCE, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> sink(
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Empty> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(METHOD_SINK, getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> pipe(
        io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_PIPE, getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class StreamingBlockingStub extends io.grpc.stub.AbstractStub<StreamingBlockingStub> {
    private StreamingBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private StreamingBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new StreamingBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<io.grpc.examples.streaming.Item> source(
        io.grpc.examples.streaming.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_SOURCE, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class StreamingFutureStub extends io.grpc.stub.AbstractStub<StreamingFutureStub> {
    private StreamingFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private StreamingFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new StreamingFutureStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static abstract class StreamingVertxImplBase implements io.grpc.BindableService {

    /**
     */
    public void source(io.grpc.examples.streaming.Empty request,
        io.vertx.grpc.GrpcWriteStream<io.grpc.examples.streaming.Item> response) {
      asyncUnimplementedUnaryCall(METHOD_SOURCE, response.writeObserver());
    }

    /**
     */
    public void sink(io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item> request,
        io.vertx.core.Future<io.grpc.examples.streaming.Empty> response) {
      request.setReadObserver(asyncUnimplementedStreamingCall(METHOD_SINK, StreamingGrpc.toObserver(response.completer())));
    }

    /**
     */
    public void pipe(
        io.vertx.grpc.GrpcBidiExchange<io.grpc.examples.streaming.Item, io.grpc.examples.streaming.Item> exchange) {
      exchange.setReadObserver(asyncUnimplementedStreamingCall(METHOD_PIPE, exchange.writeObserver()));
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SOURCE,
            asyncServerStreamingCall(
              new VertxMethodHandlers<
                io.grpc.examples.streaming.Empty,
                io.grpc.examples.streaming.Item>(
                  this, METHODID_SOURCE)))
          .addMethod(
            METHOD_SINK,
            asyncClientStreamingCall(
              new VertxMethodHandlers<
                io.grpc.examples.streaming.Item,
                io.grpc.examples.streaming.Empty>(
                  this, METHODID_SINK)))
          .addMethod(
            METHOD_PIPE,
            asyncBidiStreamingCall(
              new VertxMethodHandlers<
                io.grpc.examples.streaming.Item,
                io.grpc.examples.streaming.Item>(
                  this, METHODID_PIPE)))
          .build();
    }
  }

  /**
   * <pre>
   * Interface exported by the server.
   * </pre>
   */
  public static final class StreamingVertxStub extends io.grpc.stub.AbstractStub<StreamingVertxStub> {
    private StreamingVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    private StreamingVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new StreamingVertxStub(channel, callOptions);
    }

    /**
     */
    public void source(io.grpc.examples.streaming.Empty request,
        io.vertx.core.Handler<io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.examples.streaming.Item>create();

      handler.handle(readStream);
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_SOURCE, getCallOptions()), request, readStream.readObserver());
    }

    /**
     */
    public void sink(io.vertx.core.Handler<
        io.vertx.grpc.GrpcUniExchange<io.grpc.examples.streaming.Item, io.grpc.examples.streaming.Empty>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Empty> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.examples.streaming.Empty>create();

      handler.handle(io.vertx.grpc.GrpcUniExchange.create(readStream, asyncClientStreamingCall(
          getChannel().newCall(METHOD_SINK, getCallOptions()), readStream.readObserver())));
    }

    /**
     */
    public void pipe(io.vertx.core.Handler<
        io.vertx.grpc.GrpcBidiExchange<io.grpc.examples.streaming.Item, io.grpc.examples.streaming.Item>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.examples.streaming.Item>create();

      handler.handle(io.vertx.grpc.GrpcBidiExchange.create(readStream, asyncBidiStreamingCall(
          getChannel().newCall(METHOD_PIPE, getCallOptions()), readStream.readObserver())));
    }
  }

  private static final int METHODID_SOURCE = 0;
  private static final int METHODID_SINK = 1;
  private static final int METHODID_PIPE = 2;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final StreamingImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(StreamingImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SOURCE:
          serviceImpl.source((io.grpc.examples.streaming.Empty) request,
              (io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item>) responseObserver);
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
        case METHODID_SINK:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sink(
              (io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Empty>) responseObserver);
        case METHODID_PIPE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.pipe(
              (io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item>) responseObserver);
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
    private final StreamingVertxImplBase serviceImpl;
    private final int methodId;

    public VertxMethodHandlers(StreamingVertxImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SOURCE:
          serviceImpl.source((io.grpc.examples.streaming.Empty) request,
              (io.vertx.grpc.GrpcWriteStream<io.grpc.examples.streaming.Item>) io.vertx.grpc.GrpcWriteStream.create(responseObserver));
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
        case METHODID_SINK:
          io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item> request1 = io.vertx.grpc.GrpcReadStream.<io.grpc.examples.streaming.Item>create();
          serviceImpl.sink(request1, (io.vertx.core.Future<io.grpc.examples.streaming.Empty>) io.vertx.core.Future.<io.grpc.examples.streaming.Empty>future().setHandler(ar -> {
            if (ar.succeeded()) {
              ((io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Empty>) responseObserver).onNext(ar.result());
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(ar.cause());
            }
          }));
          return (io.grpc.stub.StreamObserver<Req>) request1.readObserver();
        case METHODID_PIPE:
          io.vertx.grpc.GrpcReadStream<io.grpc.examples.streaming.Item> request2 = io.vertx.grpc.GrpcReadStream.<io.grpc.examples.streaming.Item>create();
          serviceImpl.pipe(
             io.vertx.grpc.GrpcBidiExchange.<io.grpc.examples.streaming.Item, io.grpc.examples.streaming.Item>create(
               request2,
               (io.grpc.stub.StreamObserver<io.grpc.examples.streaming.Item>) responseObserver));
          return (io.grpc.stub.StreamObserver<Req>) request2.readObserver();
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class StreamingDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.streaming.StreamingProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (StreamingGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StreamingDescriptorSupplier())
              .addMethod(METHOD_SOURCE)
              .addMethod(METHOD_SINK)
              .addMethod(METHOD_PIPE)
              .build();
        }
      }
    }
    return result;
  }
}
