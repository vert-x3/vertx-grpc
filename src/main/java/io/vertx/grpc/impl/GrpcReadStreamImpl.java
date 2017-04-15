package io.vertx.grpc.impl;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.grpc.GrpcReadStream;

import java.util.ArrayDeque;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class GrpcReadStreamImpl<T> implements GrpcReadStream<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcReadStreamImpl.class);

  private static final Throwable COMPLETED_SENTINEL = new Throwable();
  private static final int FETCH_SIZE = 8;

  private Handler<T> streamHandler;
  private Handler<Throwable> errorHandler;
  private Handler<Void> endHandler;
  private StreamObserver<T> observer;
  private boolean paused;
  private ArrayDeque<T> pending = new ArrayDeque<>();
  private ClientCallStreamObserver<Object> requestStream;
  private Throwable end;


  public GrpcReadStreamImpl(StreamObserver<T> observer) {
    this.observer = observer;
  }

  public GrpcReadStreamImpl() {
  }

  @Override
  public GrpcReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  @Override
  public GrpcReadStream<T> handler(Handler<T> handler) {
    this.streamHandler = handler;
    return this;
  }

  @Override
  public GrpcReadStream<T> pause() {
    paused = true;
    return this;
  }

  @Override
  public GrpcReadStream<T> resume() {
    paused = false;
    deliverPending();
    return this;
  }

  @Override
  public GrpcReadStream<T> endHandler(Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  private void deliverPending() {
    if (!paused) {
      T value = pending.poll();
      if (value != null) {
        if (pending.isEmpty()) {
          requestStream.request(FETCH_SIZE);
        } else {
          // Might bug ?
          Vertx.currentContext().runOnContext(v -> {
            deliverPending();
          });
        }
        if (streamHandler != null) {
          streamHandler.handle(value);
        }
      } else {
        if (end != null) {
          if (end == COMPLETED_SENTINEL) {
            Handler<Void> handler = this.endHandler;
            if (handler != null) {
              handler.handle(null);
            }
          } else {
            Handler<Throwable> handler = this.errorHandler;
            if (handler != null) {
              handler.handle(end);
            } else {
              LOG.error(end.getMessage(), end);
            }
          }
        }
      }
    }
  }

  @Override
  public StreamObserver<T> readObserver() {
    if (observer == null) {
      observer = new ClientResponseObserver<Object, T>() {

        @Override
        public void onNext(T value) {
          if (requestStream != null) {
            pending.add(value);
            deliverPending();
          } else {
            if (streamHandler != null) {
              streamHandler.handle(value);
            }
          }
        }

        @Override
        public void onError(Throwable t) {
          if (requestStream != null) {
            end = t;
            deliverPending();
          } else {
            if (t == COMPLETED_SENTINEL) {
              if (endHandler != null) {
                endHandler.handle(null);
              }
            } else {
              if (errorHandler != null) {
                errorHandler.handle(t);
              }
            }
          }
        }

        @Override
        public void onCompleted() {
          onError(COMPLETED_SENTINEL);
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<Object> stream) {
          stream.disableAutoInboundFlowControl();
          requestStream = stream;
        }
      };
    }
    return observer;
  }

  @Override
  public GrpcReadStream<T> setReadObserver(StreamObserver<T> observer) {
    this.observer = observer;
    return this;
  }
}
