package io.vertx.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.vertx.core.Vertx;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps the ServerInterceptor and executes it on worker thread.
 * All incoming events will be deferred until the interception is completed.
 *
 * @author <a href="mailto:pkopachevskiy@corp.finam.ru">Pavel Kopachevskiy</a>
 * @author <a href="mailto:ruslan.sennov@gmail.com">Ruslan Sennov</a>
 */
public class BlockingServerInterceptor implements ServerInterceptor {

  public static ServerInterceptor wrap(Vertx vertx, ServerInterceptor interceptor) {
    return new BlockingServerInterceptor(vertx, interceptor);
  }

  private final Vertx vertx;
  private final ServerInterceptor interceptor;

  private BlockingServerInterceptor(Vertx vertx, ServerInterceptor interceptor) {
    this.vertx = vertx;
    this.interceptor = interceptor;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                                                               ServerCallHandler<ReqT, RespT> serverCallHandler) {
    AsyncListener<ReqT> asyncListener = new AsyncListener<>();
    vertx.<ServerCall.Listener<ReqT>>executeBlocking(blockingCode ->
        blockingCode.complete(interceptor.interceptCall(serverCall, metadata, serverCallHandler)),
      ar -> {
        if (ar.succeeded()) {
          asyncListener.setDelegate(ar.result());
        } else {
          asyncListener.setDelegate(new ServerCall.Listener<ReqT>() {
          });
        }
      });
    return asyncListener;
  }

  /**
   * Saves incoming events to queue until the delegated listener is presented
   */
  private static class AsyncListener<ReqT> extends ServerCall.Listener<ReqT> {
    private ServerCall.Listener<ReqT> delegate;
    private List<Consumer<ServerCall.Listener<ReqT>>> incomingEvents = new LinkedList<>();

    void setDelegate(ServerCall.Listener<ReqT> delegate) {
      this.delegate = delegate;
      for (Consumer<ServerCall.Listener<ReqT>> event : incomingEvents) {
        event.accept(delegate);
      }
      incomingEvents.clear();
    }

    private void runIfPresent(Consumer<ServerCall.Listener<ReqT>> consumer) {
      if (this.delegate != null) {
        consumer.accept(delegate);
      } else {
        incomingEvents.add(consumer);
      }
    }

    @Override
    public void onMessage(ReqT message) {
      runIfPresent(t -> t.onMessage(message));
    }

    @Override
    public void onHalfClose() {
      runIfPresent(ServerCall.Listener::onHalfClose);
    }

    @Override
    public void onCancel() {
      runIfPresent(ServerCall.Listener::onCancel);
    }

    @Override
    public void onComplete() {
      runIfPresent(ServerCall.Listener::onComplete);
    }

    @Override
    public void onReady() {
      runIfPresent(ServerCall.Listener::onReady);
    }
  }
}
