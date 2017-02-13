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
 * A simple service to test the various types of RPCs and experiment with
 * performance with various types of payload.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.1)",
    comments = "Source: test.proto")
public class TestServiceGrpc {

  private TestServiceGrpc() {}

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

  public static final String SERVICE_NAME = "grpc.testing.TestService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.google.protobuf.EmptyProtos.Empty,
      com.google.protobuf.EmptyProtos.Empty> METHOD_EMPTY_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.TestService", "EmptyCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.testing.integration.Messages.SimpleRequest,
      io.grpc.testing.integration.Messages.SimpleResponse> METHOD_UNARY_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.TestService", "UnaryCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
      io.grpc.testing.integration.Messages.StreamingOutputCallResponse> METHOD_STREAMING_OUTPUT_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "grpc.testing.TestService", "StreamingOutputCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.testing.integration.Messages.StreamingInputCallRequest,
      io.grpc.testing.integration.Messages.StreamingInputCallResponse> METHOD_STREAMING_INPUT_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
          generateFullMethodName(
              "grpc.testing.TestService", "StreamingInputCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingInputCallRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingInputCallResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
      io.grpc.testing.integration.Messages.StreamingOutputCallResponse> METHOD_FULL_DUPLEX_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "grpc.testing.TestService", "FullDuplexCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
      io.grpc.testing.integration.Messages.StreamingOutputCallResponse> METHOD_HALF_DUPLEX_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "grpc.testing.TestService", "HalfDuplexCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.google.protobuf.EmptyProtos.Empty,
      com.google.protobuf.EmptyProtos.Empty> METHOD_UNIMPLEMENTED_CALL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "grpc.testing.TestService", "UnimplementedCall"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.google.protobuf.EmptyProtos.Empty.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TestServiceStub newStub(io.grpc.Channel channel) {
    return new TestServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TestServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new TestServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static TestServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new TestServiceFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static TestServiceVertxStub newVertxStub(io.grpc.Channel channel) {
    return new TestServiceVertxStub(channel);
  }

  /**
   * <pre>
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static abstract class TestServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public void emptyCall(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EMPTY_CALL, responseObserver);
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public void unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.SimpleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_UNARY_CALL, responseObserver);
    }

    /**
     * <pre>
     * One request followed by a sequence of responses (streamed download).
     * The server returns the payload with client desired type and sizes.
     * </pre>
     */
    public void streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_STREAMING_OUTPUT_CALL, responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests followed by one response (streamed upload).
     * The server returns the aggregated size of client payload as the result.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallRequest> streamingInputCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_STREAMING_INPUT_CALL, responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests with each request served by the server immediately.
     * As one request could lead to multiple responses, this interface
     * demonstrates the idea of full duplexing.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> fullDuplexCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_FULL_DUPLEX_CALL, responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests followed by a sequence of responses.
     * The server buffers all the client requests and then serves them in order. A
     * stream of responses are returned to the client when the server starts with
     * first request.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> halfDuplexCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_HALF_DUPLEX_CALL, responseObserver);
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_UNIMPLEMENTED_CALL, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_EMPTY_CALL,
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_EMPTY_CALL)))
          .addMethod(
            METHOD_UNARY_CALL,
            asyncUnaryCall(
              new MethodHandlers<
                io.grpc.testing.integration.Messages.SimpleRequest,
                io.grpc.testing.integration.Messages.SimpleResponse>(
                  this, METHODID_UNARY_CALL)))
          .addMethod(
            METHOD_STREAMING_OUTPUT_CALL,
            asyncServerStreamingCall(
              new MethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_STREAMING_OUTPUT_CALL)))
          .addMethod(
            METHOD_STREAMING_INPUT_CALL,
            asyncClientStreamingCall(
              new MethodHandlers<
                io.grpc.testing.integration.Messages.StreamingInputCallRequest,
                io.grpc.testing.integration.Messages.StreamingInputCallResponse>(
                  this, METHODID_STREAMING_INPUT_CALL)))
          .addMethod(
            METHOD_FULL_DUPLEX_CALL,
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_FULL_DUPLEX_CALL)))
          .addMethod(
            METHOD_HALF_DUPLEX_CALL,
            asyncBidiStreamingCall(
              new MethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_HALF_DUPLEX_CALL)))
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
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static final class TestServiceStub extends io.grpc.stub.AbstractStub<TestServiceStub> {
    private TestServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public void emptyCall(com.google.protobuf.EmptyProtos.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EMPTY_CALL, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public void unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.SimpleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UNARY_CALL, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * One request followed by a sequence of responses (streamed download).
     * The server returns the payload with client desired type and sizes.
     * </pre>
     */
    public void streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request,
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_STREAMING_OUTPUT_CALL, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests followed by one response (streamed upload).
     * The server returns the aggregated size of client payload as the result.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallRequest> streamingInputCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallResponse> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(METHOD_STREAMING_INPUT_CALL, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests with each request served by the server immediately.
     * As one request could lead to multiple responses, this interface
     * demonstrates the idea of full duplexing.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> fullDuplexCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_FULL_DUPLEX_CALL, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * A sequence of requests followed by a sequence of responses.
     * The server buffers all the client requests and then serves them in order. A
     * stream of responses are returned to the client when the server starts with
     * first request.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> halfDuplexCall(
        io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_HALF_DUPLEX_CALL, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
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
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static final class TestServiceBlockingStub extends io.grpc.stub.AbstractStub<TestServiceBlockingStub> {
    private TestServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public com.google.protobuf.EmptyProtos.Empty emptyCall(com.google.protobuf.EmptyProtos.Empty request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EMPTY_CALL, getCallOptions(), request);
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public io.grpc.testing.integration.Messages.SimpleResponse unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_UNARY_CALL, getCallOptions(), request);
    }

    /**
     * <pre>
     * One request followed by a sequence of responses (streamed download).
     * The server returns the payload with client desired type and sizes.
     * </pre>
     */
    public java.util.Iterator<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(
        io.grpc.testing.integration.Messages.StreamingOutputCallRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_STREAMING_OUTPUT_CALL, getCallOptions(), request);
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
     * </pre>
     */
    public com.google.protobuf.EmptyProtos.Empty unimplementedCall(com.google.protobuf.EmptyProtos.Empty request) {
      return blockingUnaryCall(
          getChannel(), METHOD_UNIMPLEMENTED_CALL, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static final class TestServiceFutureStub extends io.grpc.stub.AbstractStub<TestServiceFutureStub> {
    private TestServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.EmptyProtos.Empty> emptyCall(
        com.google.protobuf.EmptyProtos.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EMPTY_CALL, getCallOptions()), request);
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(
        io.grpc.testing.integration.Messages.SimpleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_UNARY_CALL, getCallOptions()), request);
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
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
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static abstract class TestServiceVertxImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public void emptyCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty> response) {
      asyncUnimplementedUnaryCall(METHOD_EMPTY_CALL, TestServiceGrpc.toObserver(response.completer()));
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public void unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request,
        io.vertx.core.Future<io.grpc.testing.integration.Messages.SimpleResponse> response) {
      asyncUnimplementedUnaryCall(METHOD_UNARY_CALL, TestServiceGrpc.toObserver(response.completer()));
    }

    /**
     * <pre>
     * One request followed by a sequence of responses (streamed download).
     * The server returns the payload with client desired type and sizes.
     * </pre>
     */
    public void streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request,
        io.vertx.grpc.GrpcWriteStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> response) {
      asyncUnimplementedUnaryCall(METHOD_STREAMING_OUTPUT_CALL, response.writeObserver());
    }

    /**
     * <pre>
     * A sequence of requests followed by one response (streamed upload).
     * The server returns the aggregated size of client payload as the result.
     * </pre>
     */
    public void streamingInputCall(io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request,
        io.vertx.core.Future<io.grpc.testing.integration.Messages.StreamingInputCallResponse> response) {
      request.setReadObserver(asyncUnimplementedStreamingCall(METHOD_STREAMING_INPUT_CALL, TestServiceGrpc.toObserver(response.completer())));
    }

    /**
     * <pre>
     * A sequence of requests with each request served by the server immediately.
     * As one request could lead to multiple responses, this interface
     * demonstrates the idea of full duplexing.
     * </pre>
     */
    public void fullDuplexCall(
        io.vertx.grpc.GrpcBidiExchange<io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> exchange) {
      exchange.setReadObserver(asyncUnimplementedStreamingCall(METHOD_FULL_DUPLEX_CALL, exchange.writeObserver()));
    }

    /**
     * <pre>
     * A sequence of requests followed by a sequence of responses.
     * The server buffers all the client requests and then serves them in order. A
     * stream of responses are returned to the client when the server starts with
     * first request.
     * </pre>
     */
    public void halfDuplexCall(
        io.vertx.grpc.GrpcBidiExchange<io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> exchange) {
      exchange.setReadObserver(asyncUnimplementedStreamingCall(METHOD_HALF_DUPLEX_CALL, exchange.writeObserver()));
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty> response) {
      asyncUnimplementedUnaryCall(METHOD_UNIMPLEMENTED_CALL, TestServiceGrpc.toObserver(response.completer()));
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_EMPTY_CALL,
            asyncUnaryCall(
              new VertxMethodHandlers<
                com.google.protobuf.EmptyProtos.Empty,
                com.google.protobuf.EmptyProtos.Empty>(
                  this, METHODID_EMPTY_CALL)))
          .addMethod(
            METHOD_UNARY_CALL,
            asyncUnaryCall(
              new VertxMethodHandlers<
                io.grpc.testing.integration.Messages.SimpleRequest,
                io.grpc.testing.integration.Messages.SimpleResponse>(
                  this, METHODID_UNARY_CALL)))
          .addMethod(
            METHOD_STREAMING_OUTPUT_CALL,
            asyncServerStreamingCall(
              new VertxMethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_STREAMING_OUTPUT_CALL)))
          .addMethod(
            METHOD_STREAMING_INPUT_CALL,
            asyncClientStreamingCall(
              new VertxMethodHandlers<
                io.grpc.testing.integration.Messages.StreamingInputCallRequest,
                io.grpc.testing.integration.Messages.StreamingInputCallResponse>(
                  this, METHODID_STREAMING_INPUT_CALL)))
          .addMethod(
            METHOD_FULL_DUPLEX_CALL,
            asyncBidiStreamingCall(
              new VertxMethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_FULL_DUPLEX_CALL)))
          .addMethod(
            METHOD_HALF_DUPLEX_CALL,
            asyncBidiStreamingCall(
              new VertxMethodHandlers<
                io.grpc.testing.integration.Messages.StreamingOutputCallRequest,
                io.grpc.testing.integration.Messages.StreamingOutputCallResponse>(
                  this, METHODID_HALF_DUPLEX_CALL)))
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
   * A simple service to test the various types of RPCs and experiment with
   * performance with various types of payload.
   * </pre>
   */
  public static final class TestServiceVertxStub extends io.grpc.stub.AbstractStub<TestServiceVertxStub> {
    private TestServiceVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TestServiceVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TestServiceVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TestServiceVertxStub(channel, callOptions);
    }

    /**
     * <pre>
     * One empty request followed by one empty response.
     * </pre>
     */
    public void emptyCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<com.google.protobuf.EmptyProtos.Empty>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EMPTY_CALL, getCallOptions()), request, TestServiceGrpc.toObserver(response));
    }

    /**
     * <pre>
     * One request followed by one response.
     * </pre>
     */
    public void unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<io.grpc.testing.integration.Messages.SimpleResponse>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UNARY_CALL, getCallOptions()), request, TestServiceGrpc.toObserver(response));
    }

    /**
     * <pre>
     * One request followed by a sequence of responses (streamed download).
     * The server returns the payload with client desired type and sizes.
     * </pre>
     */
    public void streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request,
        io.vertx.core.Handler<io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>create();

      handler.handle(readStream);
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_STREAMING_OUTPUT_CALL, getCallOptions()), request, readStream.readObserver());
    }

    /**
     * <pre>
     * A sequence of requests followed by one response (streamed upload).
     * The server returns the aggregated size of client payload as the result.
     * </pre>
     */
    public void streamingInputCall(io.vertx.core.Handler<
        io.vertx.grpc.GrpcUniExchange<io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingInputCallResponse> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingInputCallResponse>create();

      handler.handle(io.vertx.grpc.GrpcUniExchange.create(readStream, asyncClientStreamingCall(
          getChannel().newCall(METHOD_STREAMING_INPUT_CALL, getCallOptions()), readStream.readObserver())));
    }

    /**
     * <pre>
     * A sequence of requests with each request served by the server immediately.
     * As one request could lead to multiple responses, this interface
     * demonstrates the idea of full duplexing.
     * </pre>
     */
    public void fullDuplexCall(io.vertx.core.Handler<
        io.vertx.grpc.GrpcBidiExchange<io.grpc.testing.integration.Messages.StreamingOutputCallResponse, io.grpc.testing.integration.Messages.StreamingOutputCallRequest>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>create();

      handler.handle(io.vertx.grpc.GrpcBidiExchange.create(readStream, asyncBidiStreamingCall(
          getChannel().newCall(METHOD_FULL_DUPLEX_CALL, getCallOptions()), readStream.readObserver())));
    }

    /**
     * <pre>
     * A sequence of requests followed by a sequence of responses.
     * The server buffers all the client requests and then serves them in order. A
     * stream of responses are returned to the client when the server starts with
     * first request.
     * </pre>
     */
    public void halfDuplexCall(io.vertx.core.Handler<
        io.vertx.grpc.GrpcBidiExchange<io.grpc.testing.integration.Messages.StreamingOutputCallResponse, io.grpc.testing.integration.Messages.StreamingOutputCallRequest>> handler) {
      final io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> readStream =
          io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>create();

      handler.handle(io.vertx.grpc.GrpcBidiExchange.create(readStream, asyncBidiStreamingCall(
          getChannel().newCall(METHOD_HALF_DUPLEX_CALL, getCallOptions()), readStream.readObserver())));
    }

    /**
     * <pre>
     * The test server will not implement this method. It will be used
     * to test the behavior when clients call unimplemented methods.
     * </pre>
     */
    public void unimplementedCall(com.google.protobuf.EmptyProtos.Empty request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<com.google.protobuf.EmptyProtos.Empty>> response) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UNIMPLEMENTED_CALL, getCallOptions()), request, TestServiceGrpc.toObserver(response));
    }
  }

  private static final int METHODID_EMPTY_CALL = 0;
  private static final int METHODID_UNARY_CALL = 1;
  private static final int METHODID_STREAMING_OUTPUT_CALL = 2;
  private static final int METHODID_UNIMPLEMENTED_CALL = 3;
  private static final int METHODID_STREAMING_INPUT_CALL = 4;
  private static final int METHODID_FULL_DUPLEX_CALL = 5;
  private static final int METHODID_HALF_DUPLEX_CALL = 6;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TestServiceImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(TestServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EMPTY_CALL:
          serviceImpl.emptyCall((com.google.protobuf.EmptyProtos.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver);
          break;
        case METHODID_UNARY_CALL:
          serviceImpl.unaryCall((io.grpc.testing.integration.Messages.SimpleRequest) request,
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.SimpleResponse>) responseObserver);
          break;
        case METHODID_STREAMING_OUTPUT_CALL:
          serviceImpl.streamingOutputCall((io.grpc.testing.integration.Messages.StreamingOutputCallRequest) request,
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) responseObserver);
          break;
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
        case METHODID_STREAMING_INPUT_CALL:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamingInputCall(
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallResponse>) responseObserver);
        case METHODID_FULL_DUPLEX_CALL:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.fullDuplexCall(
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) responseObserver);
        case METHODID_HALF_DUPLEX_CALL:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.halfDuplexCall(
              (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) responseObserver);
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
    private final TestServiceVertxImplBase serviceImpl;
    private final int methodId;

    public VertxMethodHandlers(TestServiceVertxImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EMPTY_CALL:
          serviceImpl.emptyCall((com.google.protobuf.EmptyProtos.Empty) request,
              (io.vertx.core.Future<com.google.protobuf.EmptyProtos.Empty>) io.vertx.core.Future.<com.google.protobuf.EmptyProtos.Empty>future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<com.google.protobuf.EmptyProtos.Empty>) responseObserver).onNext(ar.result());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(ar.cause());
                }
              }));
          break;
        case METHODID_UNARY_CALL:
          serviceImpl.unaryCall((io.grpc.testing.integration.Messages.SimpleRequest) request,
              (io.vertx.core.Future<io.grpc.testing.integration.Messages.SimpleResponse>) io.vertx.core.Future.<io.grpc.testing.integration.Messages.SimpleResponse>future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.SimpleResponse>) responseObserver).onNext(ar.result());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(ar.cause());
                }
              }));
          break;
        case METHODID_STREAMING_OUTPUT_CALL:
          serviceImpl.streamingOutputCall((io.grpc.testing.integration.Messages.StreamingOutputCallRequest) request,
              (io.vertx.grpc.GrpcWriteStream<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) io.vertx.grpc.GrpcWriteStream.create(responseObserver));
          break;
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
        case METHODID_STREAMING_INPUT_CALL:
          io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request3 = io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingInputCallRequest>create();
          serviceImpl.streamingInputCall(request3, (io.vertx.core.Future<io.grpc.testing.integration.Messages.StreamingInputCallResponse>) io.vertx.core.Future.<io.grpc.testing.integration.Messages.StreamingInputCallResponse>future().setHandler(ar -> {
            if (ar.succeeded()) {
              ((io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingInputCallResponse>) responseObserver).onNext(ar.result());
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(ar.cause());
            }
          }));
          return (io.grpc.stub.StreamObserver<Req>) request3.readObserver();
        case METHODID_FULL_DUPLEX_CALL:
          io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request4 = io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingOutputCallRequest>create();
          serviceImpl.fullDuplexCall(
             io.vertx.grpc.GrpcBidiExchange.<io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>create(
               request4,
               (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) responseObserver));
          return (io.grpc.stub.StreamObserver<Req>) request4.readObserver();
        case METHODID_HALF_DUPLEX_CALL:
          io.vertx.grpc.GrpcReadStream<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request5 = io.vertx.grpc.GrpcReadStream.<io.grpc.testing.integration.Messages.StreamingOutputCallRequest>create();
          serviceImpl.halfDuplexCall(
             io.vertx.grpc.GrpcBidiExchange.<io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>create(
               request5,
               (io.grpc.stub.StreamObserver<io.grpc.testing.integration.Messages.StreamingOutputCallResponse>) responseObserver));
          return (io.grpc.stub.StreamObserver<Req>) request5.readObserver();
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class TestServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.testing.integration.Test.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TestServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TestServiceDescriptorSupplier())
              .addMethod(METHOD_EMPTY_CALL)
              .addMethod(METHOD_UNARY_CALL)
              .addMethod(METHOD_STREAMING_OUTPUT_CALL)
              .addMethod(METHOD_STREAMING_INPUT_CALL)
              .addMethod(METHOD_FULL_DUPLEX_CALL)
              .addMethod(METHOD_HALF_DUPLEX_CALL)
              .addMethod(METHOD_UNIMPLEMENTED_CALL)
              .build();
        }
      }
    }
    return result;
  }
}
