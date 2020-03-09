package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.testing.integration.VertxTestServiceGrpc;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.grpc.utils.IterableReadStream;
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
import io.vertx.core.Future;
import io.vertx.core.streams.WriteStream;

/**
 * A simple test to showcase the various types of RPCs.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GoogleTest extends GrpcTestBase {

  private ManagedChannel channel;

  private VertxTestServiceGrpc.VertxTestServiceStub buildStub() {
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port).usePlaintext(true).build();
    return VertxTestServiceGrpc.newVertxStub(channel);
  }

  /**
   * One empty request followed by one empty response.
   */
  @Test
  public void emptyCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      @Override
      public Future<Empty> emptyCall(Empty request) {
        will.assertNotNull(request);

        return Future.succeededFuture(Empty.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().emptyCall(Empty.newBuilder().build()).onComplete(res -> {
          if (res.succeeded()) {
            will.assertNotNull(res.result());
            test.complete();
            channel.shutdown();
          } else {
            will.fail(res.cause());
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
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      @Override
      public Future<SimpleResponse> unaryCall(SimpleRequest request) {
        will.assertNotNull(request);

        return Future.succeededFuture(SimpleResponse.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unaryCall(SimpleRequest.newBuilder().build()).onComplete(res -> {
          if (res.succeeded()) {
            will.assertNotNull(res.result());
            test.complete();
            channel.shutdown();
          } else {
            will.fail(res.cause());
          }
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * One request followed by a sequence of responses (streamed download). The server returns the payload with client desired type and sizes.
   */
  @Test
  public void streamingOutputCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      @Override
      public void streamingOutputCall(StreamingOutputCallRequest request, WriteStream<StreamingOutputCallResponse> response) {
        will.assertNotNull(request);
        IterableReadStream rs = new IterableReadStream(v -> StreamingOutputCallResponse.newBuilder().build(), 10);
        rs.pipeTo(response);
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().streamingOutputCall(StreamingOutputCallRequest.newBuilder().build())
         .handler(resp -> {
           will.assertNotNull(resp);
           cnt.incrementAndGet();
         })
         .exceptionHandler(will::fail)
         .endHandler(v -> {
           will.assertEquals(10, cnt.get());
           test.complete();
           channel.shutdown();
         });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests followed by one response (streamed upload). The server returns the aggregated size of client payload as the result.
   */
  @Test
  public void streamingInputCallTest(TestContext will) throws Exception {
    final Async test = will.async();
    final AtomicInteger cnt = new AtomicInteger();

    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
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
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().streamingInputCall(ws -> new IterableReadStream<>(v -> StreamingInputCallRequest.newBuilder().build(), 10).pipeTo(ws))
         .onComplete(res -> {
           if (res.succeeded()) {
             will.assertNotNull(res.result());
             test.complete();
             channel.shutdown();
           } else {
             will.fail(res.cause());
           }
         });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests with each request served by the server immediately. As one request could lead to multiple responses, this interface demonstrates the
   * idea of full duplexing.
   */
  @Test
  public void fullDuplexCallTest(TestContext will) throws Exception {
    final Async test = will.async();

    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
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
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().fullDuplexCall(ws -> new IterableReadStream<>(v -> StreamingOutputCallRequest.newBuilder().build(), 10).pipeTo(ws))
         .endHandler(v -> {
           will.assertEquals(10, cnt.get());
           test.complete();
           channel.shutdown();
         })
         .exceptionHandler(will::fail)
         .handler(item -> {
           will.assertNotNull(item);
           cnt.incrementAndGet();
         });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * A sequence of requests followed by a sequence of responses. The server buffers all the client requests and then serves them in order. A stream of responses
   * are returned to the client when the server starts with first request.
   */
  @Test
  public void halfDuplexCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
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
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        final AtomicBoolean down = new AtomicBoolean();
        buildStub().halfDuplexCall(ws -> new IterableReadStream<>(v -> StreamingOutputCallRequest.newBuilder().build(), 10).pipeTo(ws))
         .endHandler(v -> {
           will.assertEquals(10, cnt.get());
           test.complete();
           channel.shutdown();
         })
         .exceptionHandler(will::fail)
         .handler(item -> {
           will.assertTrue(down.get());
           will.assertNotNull(item);
           cnt.incrementAndGet();
         });
        // down stream is now expected
        down.set(true);
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }

  /**
   * The test server will not implement this method. It will be used to test the behavior when clients call unimplemented methods.
   */
  @Test
  public void unimplementedCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      // The test server will not implement this method. It will be used
      // to test the behavior when clients call unimplemented methods.
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unimplementedCall(Empty.newBuilder().build()).onComplete(res -> {
          if (res.succeeded()) {
            will.fail("Should not succeed, there is no implementation");
          } else {
            will.assertNotNull(res.cause());
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

}
