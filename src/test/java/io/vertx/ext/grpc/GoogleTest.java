package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.GrpcBidiExchange;
import io.vertx.grpc.GrpcReadStream;
import io.vertx.grpc.GrpcWriteStream;
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

  private TestServiceVertxStub buildStub() {
    channel = VertxChannelBuilder.forAddress(vertx, "localhost", port).usePlaintext(true).build();
    return newVertxStub(channel);
  }

  /**
   * One empty request followed by one empty response.
   */
  @Test
  public void emptyCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new TestServiceVertxImplBase() {
      @Override
      public void emptyCall(Empty request, Future<Empty> response) {
        will.assertNotNull(request);
        response.complete(Empty.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().emptyCall(Empty.newBuilder().build(), res -> {
          if (res.succeeded()) {
            will.assertNotNull(res.result());
            test.complete();
          }
          channel.shutdown();
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
    startServer(new TestServiceVertxImplBase() {
      @Override
      public void unaryCall(SimpleRequest request, Future<SimpleResponse> response) {
        will.assertNotNull(request);
        response.complete(SimpleResponse.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unaryCall(SimpleRequest.newBuilder().build(), res -> {
          if (res.succeeded()) {
            will.assertNotNull(res.result());
            test.complete();
          }
          channel.shutdown();
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
    startServer(new TestServiceVertxImplBase() {
      @Override
      public void streamingOutputCall(StreamingOutputCallRequest request, GrpcWriteStream<StreamingOutputCallResponse> response) {
        will.assertNotNull(request);
        for (int i = 0; i < 10; i++) {
          response.write(StreamingOutputCallResponse.newBuilder().build());
        }
        response.end();
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();

        buildStub().streamingOutputCall(StreamingOutputCallRequest.newBuilder().build(), exchange -> {
          exchange
            .exceptionHandler(will::fail)
            .handler(resp -> {
              will.assertNotNull(resp);
              cnt.incrementAndGet();
            })
            .endHandler(v -> {
              will.assertEquals(10, cnt.get());
              test.complete();
              channel.shutdown();
            });
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

    startServer(new TestServiceVertxImplBase() {
      @Override
      public void streamingInputCall(GrpcReadStream<StreamingInputCallRequest> request, Future<StreamingInputCallResponse> response) {
        will.assertNotNull(response);

        request
          .exceptionHandler(will::fail)
          .handler(resp -> {
            will.assertNotNull(resp);
            cnt.incrementAndGet();
          })
          .endHandler(v -> {
            will.assertEquals(10, cnt.get());
            response.complete(StreamingInputCallResponse.newBuilder().build());
          });
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().streamingInputCall(exchange -> {
          exchange
            .handler(res -> {
              if (res.failed()) {
                will.fail(res.cause());
              } else {
                will.assertNotNull(res.result());
                test.complete();
                channel.shutdown();
              }
            });

          for (int i = 0; i < 10; i++) {
            exchange.write(StreamingInputCallRequest.newBuilder().build());
          }
          exchange.end();
        });
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

    startServer(new TestServiceVertxImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public void fullDuplexCall(GrpcBidiExchange<StreamingOutputCallRequest, StreamingOutputCallResponse> exchange) {
        exchange
          .exceptionHandler(will::fail)
          .handler(item -> {
            will.assertNotNull(item);
            cnt.incrementAndGet();
            exchange.write(StreamingOutputCallResponse.newBuilder().build());
          })
          .endHandler(v -> {
            will.assertEquals(10, cnt.get());
            exchange.end();
          });

      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().fullDuplexCall(exchange -> {
          exchange
            .exceptionHandler(will::fail)
            .handler(item -> {
              will.assertNotNull(item);
              cnt.incrementAndGet();
            })
            .endHandler(v -> {
              will.assertEquals(10, cnt.get());
              test.complete();
              channel.shutdown();
            });

          for (int i = 0; i < 10; i++) {
            exchange.write(StreamingOutputCallRequest.newBuilder().build());
          }
          exchange.end();
        });
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
    startServer(new TestServiceVertxImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public void halfDuplexCall(GrpcBidiExchange<StreamingOutputCallRequest, StreamingOutputCallResponse> exchange) {
        List<StreamingOutputCallRequest> buffer = new ArrayList<>();

        exchange
          .exceptionHandler(will::fail)
          .handler(item -> {
            will.assertNotNull(item);
            cnt.incrementAndGet();
            buffer.add(item);
          })
          .endHandler(v -> {
            will.assertEquals(10, cnt.get());
            for (int i = 0; i < buffer.size(); i++) {
              exchange.write(StreamingOutputCallResponse.newBuilder().build());
            }
            exchange.end();
          });
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        final AtomicBoolean down = new AtomicBoolean();

        buildStub().halfDuplexCall(exchange -> {
          exchange
            .exceptionHandler(will::fail)
            .handler(item -> {
              will.assertTrue(down.get());
              will.assertNotNull(item);
              cnt.incrementAndGet();
            })
            .endHandler(v -> {
              will.assertEquals(10, cnt.get());
              test.complete();
              channel.shutdown();
            });

          for (int i = 0; i < 10; i++) {
            exchange.write(StreamingOutputCallRequest.newBuilder().build());
          }
          exchange.end();
          // down stream is now expected
          down.set(true);
        });
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
    startServer(new TestServiceVertxImplBase() {
      // The test server will not implement this method. It will be used
      // to test the behavior when clients call unimplemented methods.
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unimplementedCall(Empty.newBuilder().build(), res -> {
          if (res.succeeded()) {
            will.fail("Should not succeed, there is no implementation");
          } else {
            will.assertNotNull(res.cause());
          }
          test.complete();
          channel.shutdown();
        });
      } else {
        will.fail(startServer.cause());
        test.complete();
      }
    });
  }
}
