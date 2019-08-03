package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.testing.integration.VertxTestServiceGrpc;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.google.protobuf.EmptyProtos.*;
import static io.grpc.testing.integration.Messages.*;

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
      public Future<Empty> emptyCall(Future<Empty> request) {
        will.assertNotNull(request);

        return request.map(Empty.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().emptyCall(Empty.newBuilder().build()).setHandler(res -> {
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
      public Future<SimpleResponse> unaryCall(Future<SimpleRequest> request) {
        will.assertNotNull(request);

        return request.map(SimpleResponse.newBuilder().build());
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unaryCall(SimpleRequest.newBuilder().build()).setHandler(res -> {
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
   * One request followed by a sequence of responses (streamed download).
   * The server returns the payload with client desired type and sizes.
   */
  @Test
  public void streamingOutputCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      @Override
      public ReadStream<StreamingOutputCallResponse> streamingOutputCall(Future<StreamingOutputCallRequest> request) {
        will.assertNotNull(request);

        return new IterableReadStream<>(() -> StreamingOutputCallResponse.newBuilder().build());
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
   * A sequence of requests followed by one response (streamed upload).
   * The server returns the aggregated size of client payload as the result.
   */
  @Test
  public void streamingInputCallTest(TestContext will) throws Exception {
    final Async test = will.async();
    final AtomicInteger cnt = new AtomicInteger();

    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      @Override
      public Future<StreamingInputCallResponse> streamingInputCall(ReadStream<StreamingInputCallRequest> requestReadStream) {
        will.assertNotNull(requestReadStream);

        Promise<StreamingInputCallResponse> promise = Promise.promise();

        requestReadStream.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          promise.complete(StreamingInputCallResponse.newBuilder().build());
        });
        requestReadStream.exceptionHandler(will::fail);
        requestReadStream.handler(streamingInputCallRequest -> {
          will.assertNotNull(streamingInputCallRequest);
          cnt.incrementAndGet();
        });

        return promise.future();
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().streamingInputCall(new IterableReadStream<>(() -> StreamingInputCallRequest.newBuilder().build()))
          .setHandler(res -> {
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
   * A sequence of requests with each request served by the server immediately.
   * As one request could lead to multiple responses, this interface
   * demonstrates the idea of full duplexing.
   */
  @Test
  public void fullDuplexCallTest(TestContext will) throws Exception {
    final Async test = will.async();

    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public ReadStream<StreamingOutputCallResponse> fullDuplexCall(ReadStream<StreamingOutputCallRequest> request) {
        ManualReadStream result = new ManualReadStream();

        request.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          result.end();
        });
        request.exceptionHandler(will::fail);
        request.handler(item -> {
          will.assertNotNull(item);
          cnt.incrementAndGet();
          result.send();
        });

        return result;
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        buildStub().fullDuplexCall(new IterableReadStream<>(() -> StreamingOutputCallRequest.newBuilder().build()))
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
   * A sequence of requests followed by a sequence of responses.
   * The server buffers all the client requests and then serves them in order. A
   * stream of responses are returned to the client when the server starts with
   * first request.
   */
  @Test
  public void halfDuplexCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      final AtomicInteger cnt = new AtomicInteger();

      @Override
      public ReadStream<StreamingOutputCallResponse> halfDuplexCall(ReadStream<StreamingOutputCallRequest> request) {
        List<StreamingOutputCallRequest> buffer = new ArrayList<>();
        ManualReadStream result = new ManualReadStream();

        request.endHandler(v -> {
          will.assertEquals(10, cnt.get());
          for (int i = 0; i < buffer.size(); i++) {
            result.send();
          }
          result.end();
        });
        request.exceptionHandler(will::fail);
        request.handler(item -> {
          will.assertNotNull(item);
          cnt.incrementAndGet();
          buffer.add(item);
        });

        return result;
      }
    }, startServer -> {
      if (startServer.succeeded()) {
        final AtomicInteger cnt = new AtomicInteger();
        final AtomicBoolean down = new AtomicBoolean();
        buildStub().halfDuplexCall(new IterableReadStream<>(() -> StreamingOutputCallRequest.newBuilder().build()))
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
   * The test server will not implement this method. It will be used
   * to test the behavior when clients call unimplemented methods.
   */
  @Test
  public void unimplementedCallTest(TestContext will) throws Exception {
    Async test = will.async();
    startServer(new VertxTestServiceGrpc.TestServiceImplBase() {
      // The test server will not implement this method. It will be used
      // to test the behavior when clients call unimplemented methods.
    }, startServer -> {
      if (startServer.succeeded()) {
        buildStub().unimplementedCall(Empty.newBuilder().build()).setHandler(res -> {
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

  private class IterableReadStream<T> extends BaseReadStream<T> {

    private final Supplier<T> builder;

    public IterableReadStream(Supplier<T> builder) {
      this.builder = builder;
    }

    @Override
    public ReadStream<T> handler(Handler<T> handler) {
      for(int i = 0; i < 10; i++) {
        handler.handle(builder.get());
      }
      if (endHandler != null) {
        endHandler.handle(null);
      }

      return this;
    }

  }

  private class ManualReadStream extends BaseReadStream<StreamingOutputCallResponse> {

    private Handler<StreamingOutputCallResponse> handler;

    @Override
    public ReadStream<StreamingOutputCallResponse> handler(Handler<StreamingOutputCallResponse> handler) {
      this.handler = handler;

      return this;
    }

    public void end() {
      if (endHandler != null) {
        endHandler.handle(null);
      }
    }

    public void send() {
      if (handler != null) {
        handler.handle(StreamingOutputCallResponse.newBuilder().build());
      }
    }

    public void fail(Throwable throwable) {
      if (exceptionHandler != null) {
        exceptionHandler.handle(throwable);
      }
    }

  }

  private abstract class BaseReadStream<T> implements ReadStream<T> {

    protected Handler<Throwable> exceptionHandler;

    protected Handler<Void> endHandler;

    @Override
    public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;

      return this;
    }

    @Override
    public ReadStream<T> pause() {
      return this;
    }

    @Override
    public ReadStream<T> resume() {
      return this;
    }

    @Override
    public ReadStream<T> fetch(long l) {
      return this;
    }

    @Override
    public ReadStream<T> endHandler(Handler<Void> handler) {
      this.endHandler = handler;

      return this;
    }

  }

}
