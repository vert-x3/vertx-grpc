package io.vertx.grpc.client;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.impl.Utils;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * Bridge a gRPC service with a {@link io.vertx.grpc.client.GrpcClient}.
 */
public class GrpcClientChannel extends io.grpc.Channel {

  private static final int MAX_INFLIGHT_MESSAGES = 16;

  private GrpcClient client;
  private SocketAddress server;

  public GrpcClientChannel(GrpcClient client, SocketAddress server) {
    this.client = client;
    this.server = server;
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
    Executor exec = callOptions.getExecutor();

    return new ClientCall<RequestT, ResponseT>() {

      private Future<GrpcClientRequest<RequestT, ResponseT>> fut;
      private Listener<ResponseT> listener;
      private final LinkedList<ResponseT> queue = new LinkedList<>();
      private int requests = 0;
      private GrpcClientRequest<RequestT, ResponseT> request;
      private GrpcClientResponse<RequestT, ResponseT> grpcResponse;
      private Status status;
      private Metadata trailers;
      private boolean ready;

      @Override
      public synchronized boolean isReady() {
        return ready;
      }

      private void checkReady() {
        synchronized (this) {
          if (ready || request.writeQueueFull()) {
            return;
          }
          ready = true;
        }
        listener.onReady();
      }

      @Override
      public void start(Listener<ResponseT> responseListener, Metadata headers) {
        listener = responseListener;
        fut = client.request(server, methodDescriptor);
        fut.onComplete(ar1 -> {
          if (ar1.succeeded()) {
            request = ar1.result();
            request.drainHandler(v -> {
              checkReady();
            });
            Utils.writeMetadata(headers, request.headers());
            String compressor = callOptions.getCompressor();
            if (compressor != null) {
              request.encoding(compressor);
            }
            Future<GrpcClientResponse<RequestT, ResponseT>> responseFuture = request.response();
            responseFuture.onComplete(ar2 -> {
              if (ar2.succeeded()) {
                grpcResponse = ar2.result();
                Metadata responseHeaders = Utils.readMetadata(grpcResponse.headers());
                if (exec == null) {
                  responseListener.onHeaders(responseHeaders);
                } else {
                  exec.execute(() -> {
                    responseListener.onHeaders(responseHeaders);
                  });
                }
                grpcResponse.messageHandler(msg -> {
                  if (exec == null) {
                    responseListener.onMessage(msg);
                  } else {
                    synchronized (queue) {
                      // System.out.println("add " + msg);
                      queue.add(msg);
                      if (queue.size() > MAX_INFLIGHT_MESSAGES) {
                        grpcResponse.pause();
                      }
                    }
                    checkPending();
                  }
                });
                grpcResponse.endHandler(v -> {
                  Status responseStatus = Status.fromCodeValue(grpcResponse.status().code);
                  Metadata responseTrailers = Utils.readMetadata(grpcResponse.trailers());
                  if (exec == null) {
                    responseListener.onClose(responseStatus, responseTrailers);
                  } else {
                    synchronized (queue) {
                      status = responseStatus;
                      trailers = responseTrailers;
                    }
                    checkPending();
                  }
                });
              }
            });
            checkReady();
          } else {

          }
        });
      }

      private void checkPending() {
        boolean resume = false;
        while (true) {
          Runnable cmd;
          synchronized (queue) {
            if (queue.isEmpty()) {
              Status responseStatus = status;
              if (responseStatus == null) {
                break;
              }
              Metadata responseTrailers = trailers;
              status = null;
              trailers = null;
              cmd = () -> {
                listener.onClose(responseStatus, responseTrailers);
              };
            } else {
              if (requests == 0) {
                break;
              }
              if (queue.size() == MAX_INFLIGHT_MESSAGES) {
                resume = true;
              }
              requests--;
              ResponseT msg = queue.poll();
              cmd = () -> {
                listener.onMessage(msg);
              };
            }
          }
          if (resume) {
            grpcResponse.resume();
          }
          exec.execute(cmd);
        }
      }

      @Override
      public void request(int numMessages) {
        synchronized (queue) {
          requests += numMessages;
        }
        checkPending();
      }

      @Override
      public void cancel(@Nullable String message, @Nullable Throwable cause) {
        fut.onSuccess(req -> {
          req.reset();
        });
      }

      @Override
      public void halfClose() {
        fut.onSuccess(req -> {
          req.end();
        });
      }

      @Override
      public void sendMessage(RequestT message) {
        fut.onSuccess(v -> {
          request.write(message);
          synchronized (this) {
            ready = !request.writeQueueFull();
          }
        });
      }
    };
  }

  @Override
  public String authority() {
    return null;
  }
}
