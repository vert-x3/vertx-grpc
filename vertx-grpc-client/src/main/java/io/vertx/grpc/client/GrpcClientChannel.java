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
import java.util.concurrent.Executor;

/**
 * Bridge a gRPC service with a {@link io.vertx.grpc.client.GrpcClient}.
 */
public class GrpcClientChannel extends io.grpc.Channel {

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
      @Override
      public void start(Listener<ResponseT> responseListener, Metadata headers) {
        fut = client.request(server, methodDescriptor);
        fut.onComplete(ar1 -> {
          if (ar1.succeeded()) {
            GrpcClientRequest<RequestT, ResponseT> request = ar1.result();
            Utils.writeMetadata(headers, request.headers());
            String compressor = callOptions.getCompressor();
            if (compressor != null) {
              request.encoding(compressor);
            }
            Future<GrpcClientResponse<RequestT, ResponseT>> responseFuture = request.response();
            responseFuture.onComplete(ar2 -> {
              if (ar2.succeeded()) {
                GrpcClientResponse<RequestT, ResponseT> response = ar2.result();
                Metadata responseHeaders = Utils.readMetadata(response.headers());
                if (exec == null) {
                  responseListener.onHeaders(responseHeaders);
                } else {
                  exec.execute(() -> {
                    responseListener.onHeaders(responseHeaders);
                  });
                }
                response.messageHandler(msg -> {
                  if (exec == null) {
                    responseListener.onMessage(msg);
                  } else {
                    exec.execute(() -> {
                      responseListener.onMessage(msg);
                    });
                  }
                });
                response.endHandler(v -> {
                  Status responseStatus = Status.fromCodeValue(response.status().code);
                  Metadata responseTrailers = Utils.readMetadata(response.trailers());
                  if (exec == null) {
                    responseListener.onClose(responseStatus, responseTrailers);
                  } else {
                    exec.execute(() -> {
                      responseListener.onClose(responseStatus, responseTrailers);
                    });
                  }
                });
              }
            });
            responseListener.onReady();
          } else {

          }
        });
      }
      @Override
      public void request(int numMessages) {
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
        fut.onSuccess(req -> {
          req.write(message);
        });
      }
    };
  }

  @Override
  public String authority() {
    return null;
  }
}
