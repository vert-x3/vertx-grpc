package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.testing.integration.VertxTestServiceGrpc;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.protobuf.EmptyProtos.Empty;
import static io.grpc.testing.integration.Messages.*;

/**
 * A simple test to showcase the various types of RPCs.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GoogleTest extends GrpcTestBase {

  private ManagedChannel channel;

  private VertxTestServiceGrpc.TestServiceVertxStub buildStub() {
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port).usePlaintext().build();
    return VertxTestServiceGrpc.newVertxStub(channel);
  }

  @Override
  public void tearDown(TestContext should) {
    channel.shutdown();
    super.tearDown(should);
  }

  /**
   * One empty request followed by one empty response.
   */
  @Test
  public void emptyCallTest(TestContext will) {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      @Override
      public Future<Empty> emptyCall(Empty request) {
        will.assertNotNull(request);

        return Future.succeededFuture(Empty.newBuilder().build());
      }
    })
      .onFailure(will::fail)
      .onSuccess(v -> buildStub()
        .emptyCall(Empty.newBuilder().build())
        .onComplete(will.asyncAssertSuccess(res -> {
          will.assertNotNull(res);
          test.complete();
        })));
  }

  /**
   * One request followed by one response.
   */
  @Test
  public void emptyUnaryTest(TestContext will) {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      @Override
      public Future<SimpleResponse> unaryCall(SimpleRequest request) {
        will.assertNotNull(request);

        return Future.succeededFuture(SimpleResponse.newBuilder().build());
      }
    })
      .onFailure(will::fail)
      .onSuccess(v -> buildStub()
        .unaryCall(SimpleRequest.newBuilder().build())
        .onComplete(will.asyncAssertSuccess(res -> {
          will.assertNotNull(res);
          test.complete();
        })));
  }

  /**
   * One request followed by a sequence of responses (streamed download). The server returns the payload with client desired type and sizes.
   */
  @Test
  public void streamingOutputCallTest(TestContext will) {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      @Override
      public void streamingOutputCall(StreamingOutputCallRequest request, WriteStream<StreamingOutputCallResponse> response) {
        will.assertNotNull(request);
        IterableReadStream rs = new IterableReadStream(v -> StreamingOutputCallResponse.newBuilder().build(), 10);
        rs.pipeTo(response);
      }
    })
      .onFailure(will::fail)
      .onSuccess(v1 -> {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().streamingOutputCall(StreamingOutputCallRequest.newBuilder().build())
          .handler(resp -> {
            will.assertNotNull(resp);
            cnt.incrementAndGet();
          })
          .exceptionHandler(will::fail)
          .endHandler(v2 -> {
            will.assertEquals(10, cnt.get());
            test.complete();
          });
      });
  }

  /**
   * A sequence of requests followed by one response (streamed upload). The server returns the aggregated size of client payload as the result.
   */
  @Test
  public void streamingInputCallTest(TestContext will) {
    final Async test = will.async();
    final AtomicInteger cnt = new AtomicInteger();

    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      @Override
      public Future<StreamingInputCallResponse> streamingInputCall(ReadStream<StreamingInputCallRequest> request) {
        will.assertNotNull(request);

        Promise<StreamingInputCallResponse> promise = Promise.promise();
        request.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          promise.complete(StreamingInputCallResponse.newBuilder().build());
        });
        request.exceptionHandler(will::fail);
        request.handler(streamingInputCallRequest -> {
          will.assertNotNull(streamingInputCallRequest);
          cnt.incrementAndGet();
        });
        return promise.future();
      }
    })
      .onFailure(will::fail)
      .onSuccess(v1 -> buildStub()
        .streamingInputCall(ws -> new IterableReadStream<>(v2 -> StreamingInputCallRequest.newBuilder().build(), 10).pipeTo(ws))
        .onComplete(will.asyncAssertSuccess(res -> {
          will.assertNotNull(res);
          test.complete();
        })));
  }

  /**
   * A sequence of requests with each request served by the server immediately. As one request could lead to multiple responses, this interface demonstrates the
   * idea of full duplexing.
   */
  @Test
  public void fullDuplexCallTest(TestContext will) {
    final Async test = will.async();

    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public void fullDuplexCall(ReadStream<StreamingOutputCallRequest> request, WriteStream<StreamingOutputCallResponse> response) {

        request.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          response.end();
        });
        request.exceptionHandler(will::fail);
        request.handler(item -> {
          will.assertNotNull(item);
          cnt.incrementAndGet();
          response.write(StreamingOutputCallResponse.newBuilder().build());
        });

      }
    })
      .onFailure(will::fail)
      .onSuccess(v1 -> {
      final AtomicInteger cnt = new AtomicInteger();
      buildStub().fullDuplexCall(ws -> new IterableReadStream<>(v -> StreamingOutputCallRequest.newBuilder().build(), 10).pipeTo(ws))
        .endHandler(v2 -> {
          will.assertEquals(10, cnt.get());
          test.complete();
        })
        .exceptionHandler(will::fail)
        .handler(item -> {
          will.assertNotNull(item);
          cnt.incrementAndGet();
        });
    });
  }

  /**
   * A sequence of requests followed by a sequence of responses. The server buffers all the client requests and then serves them in order. A stream of responses
   * are returned to the client when the server starts with first request.
   */
  @Test
  public void halfDuplexCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public void halfDuplexCall(ReadStream<StreamingOutputCallRequest> request, WriteStream<StreamingOutputCallResponse> response) {
        List<StreamingOutputCallRequest> buffer = new ArrayList<>();

        request.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          for (int i = 0; i < buffer.size(); i++) {
            response.write(StreamingOutputCallResponse.newBuilder().build());
          }
          response.end();
        });
        request.exceptionHandler(will::fail);
        request.handler(item -> {
          will.assertNotNull(item);
          cnt.incrementAndGet();
          buffer.add(item);
        });
      }
    })
      .onFailure(will::fail)
      .onSuccess(v1 -> {
      final AtomicInteger cnt = new AtomicInteger();
      final AtomicBoolean down = new AtomicBoolean();
      buildStub().halfDuplexCall(ws -> new IterableReadStream<>(v2 -> StreamingOutputCallRequest.newBuilder().build(), 10).pipeTo(ws))
        .endHandler(v -> {
          will.assertEquals(10, cnt.get());
          test.complete();
        })
        .exceptionHandler(will::fail)
        .handler(item -> {
          will.assertTrue(down.get());
          will.assertNotNull(item);
          cnt.incrementAndGet();
        });
      // down stream is now expected
      down.set(true);
    });
  }

  /**
   * The test server will not implement this method. It will be used to test the behavior when clients call unimplemented methods.
   */
  @Test
  public void unimplementedCallTest(TestContext will) {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceVertxImplBase() {
      // The test server will not implement this method. It will be used
      // to test the behavior when clients call unimplemented methods.
    })
      .onFailure(will::fail)
      .onSuccess(v -> buildStub()
        .unimplementedCall(Empty.newBuilder().build())
        .onComplete(will.asyncAssertFailure(err -> {
          will.assertNotNull(err);
          test.complete();
        })));
  }
}
