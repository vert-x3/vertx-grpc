package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.protobuf.EmptyProtos.*;
import static io.grpc.testing.integration.Messages.*;
import static io.grpc.testing.integration.TestServiceGrpc.*;

/**
 * A simple test to showcase the various types of RPCs.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GoogleTest extends GrpcTestBase {

  private ManagedChannel channel;

  private TestServiceStub buildStub() {
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port).usePlaintext(true).build();
    return newStub(channel);
  }

  /**
   * One empty request followed by one empty response.
   */
  @Test
  public void emptyCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceImplBase() {
      @Override
      public void emptyCall(Empty request, StreamObserver<Empty> responseObserver) {
        will.assertNotNull(request);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().emptyCall(Empty.newBuilder().build(), new StreamObserver<Empty>() {
          private Empty result;
          @Override
          public void onNext(Empty empty) {
            result = empty;
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertNotNull(result);
            test.complete();
            channel.shutdown();
          }
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * One request followed by one response.
   */
  @Test
  public void emptyUnaryTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceImplBase() {
      @Override
      public void unaryCall(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        will.assertNotNull(request);
        responseObserver.onNext(SimpleResponse.newBuilder().build());
        responseObserver.onCompleted();
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unaryCall(SimpleRequest.newBuilder().build(), new StreamObserver<SimpleResponse>() {
          private SimpleResponse result;
          @Override
          public void onNext(SimpleResponse simpleResponse) {
            result = simpleResponse;
          }

          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertNotNull(result);
            test.complete();
            channel.shutdown();
          }
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * One request followed by a sequence of responses (streamed download).
   * The server returns the payload with client desired type and sizes.
   */
  @Test
  public void streamingOutputCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceImplBase() {
      @Override
      public void streamingOutputCall(StreamingOutputCallRequest request, StreamObserver<StreamingOutputCallResponse> responseObserver) {
        will.assertNotNull(request);
        for (int i = 0; i < 10; i++) {
          responseObserver.onNext(StreamingOutputCallResponse.newBuilder().build());
        }
        responseObserver.onCompleted();
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().streamingOutputCall(StreamingOutputCallRequest.newBuilder().build(), new StreamObserver<StreamingOutputCallResponse>() {
          @Override
          public void onNext(StreamingOutputCallResponse resp) {
            will.assertNotNull(resp);
            cnt.incrementAndGet();
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            test.complete();
            channel.shutdown();
          }
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests followed by one response (streamed upload).
   * The server returns the aggregated size of client payload as the result.
   */
  @Test
  public void streamingInputCallTest(TestContext will) throws Exception {
    final Async test = will.async();
    final AtomicInteger cnt = new AtomicInteger();

    startServer(new TestServiceImplBase() {
      @Override
      public StreamObserver<StreamingInputCallRequest> streamingInputCall(StreamObserver<StreamingInputCallResponse> responseObserver) {
        will.assertNotNull(responseObserver);
        return new StreamObserver<StreamingInputCallRequest>() {
          @Override
          public void onNext(StreamingInputCallRequest streamingInputCallRequest) {
            will.assertNotNull(streamingInputCallRequest);
            cnt.incrementAndGet();
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            responseObserver.onNext(StreamingInputCallResponse.newBuilder().build());
            responseObserver.onCompleted();
          }
        };
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        StreamObserver<StreamingInputCallRequest> val = buildStub().streamingInputCall(new StreamObserver<StreamingInputCallResponse>() {
          private StreamingInputCallResponse result;
          @Override
          public void onNext(StreamingInputCallResponse streamingInputCallResponse) {
            result = streamingInputCallResponse;
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertNotNull(result);
            test.complete();
            channel.shutdown();
          }
        });
        for (int i = 0; i < 10; i++) {
          val.onNext(StreamingInputCallRequest.newBuilder().build());
        }
        val.onCompleted();
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests with each request served by the server immediately.
   * As one request could lead to multiple responses, this interface
   * demonstrates the idea of full duplexing.
   */
  @Test
  public void fullDuplexCallTest(TestContext will) throws Exception {
    final Async test = will.async();

    startServer(new TestServiceImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public StreamObserver<StreamingOutputCallRequest> fullDuplexCall(StreamObserver<StreamingOutputCallResponse> responseObserver) {
        return new StreamObserver<StreamingOutputCallRequest>() {
          @Override
          public void onNext(StreamingOutputCallRequest item) {
            will.assertNotNull(item);
            cnt.incrementAndGet();
            responseObserver.onNext(StreamingOutputCallResponse.newBuilder().build());
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            responseObserver.onCompleted();
          }
        };
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        StreamObserver<StreamingOutputCallRequest> val = buildStub().fullDuplexCall(new StreamObserver<StreamingOutputCallResponse>() {
          @Override
          public void onNext(StreamingOutputCallResponse item) {
            will.assertNotNull(item);
            cnt.incrementAndGet();
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            test.complete();
            channel.shutdown();
          }
        });
        for (int i = 0; i < 10; i++) {
          val.onNext(StreamingOutputCallRequest.newBuilder().build());
        }
        val.onCompleted();
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests followed by a sequence of responses.
   * The server buffers all the client requests and then serves them in order. A
   * stream of responses are returned to the client when the server starts with
   * first request.
   */
  @Test
  public void halfDuplexCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public StreamObserver<StreamingOutputCallRequest> halfDuplexCall(StreamObserver<StreamingOutputCallResponse> responseObserver) {
        List<StreamingOutputCallRequest> buffer = new ArrayList<>();
        return new StreamObserver<StreamingOutputCallRequest>() {
          @Override
          public void onNext(StreamingOutputCallRequest item) {
            will.assertNotNull(item);
            cnt.incrementAndGet();
            buffer.add(item);
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            for (int i = 0; i < buffer.size(); i++) {
              responseObserver.onNext(StreamingOutputCallResponse.newBuilder().build());
            }
            responseObserver.onCompleted();
          }
        };
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        final AtomicBoolean down = new AtomicBoolean();
        StreamObserver<StreamingOutputCallRequest> val = buildStub().halfDuplexCall(new StreamObserver<StreamingOutputCallResponse>() {
          @Override
          public void onNext(StreamingOutputCallResponse item) {
            will.assertTrue(down.get());
            will.assertNotNull(item);
            cnt.incrementAndGet();
          }
          @Override
          public void onError(Throwable throwable) {
            will.fail(throwable);
          }
          @Override
          public void onCompleted() {
            will.assertEquals(10, cnt.get());
            test.complete();
            channel.shutdown();
          }
        });
        for (int i = 0; i < 10; i++) {
          val.onNext(StreamingOutputCallRequest.newBuilder().build());
        }
        val.onCompleted();
        // down stream is now expected
        down.set(true);
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * The test server will not implement this method. It will be used
   * to test the behavior when clients call unimplemented methods.
   */
  @Test
  public void unimplementedCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceImplBase() {
      // The test server will not implement this method. It will be used
      // to test the behavior when clients call unimplemented methods.
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unimplementedCall(Empty.newBuilder().build(), new StreamObserver<Empty>() {
          @Override
          public void onNext(Empty empty) {
          }
          @Override
          public void onError(Throwable throwable) {
            will.assertNotNull(throwable);
            test.complete();
            channel.shutdown();
          }
          @Override
          public void onCompleted() {
            will.fail("Should not succeed, there is no implementation");
          }
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }
}
