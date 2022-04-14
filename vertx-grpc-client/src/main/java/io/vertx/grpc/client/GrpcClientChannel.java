package io.vertx.grpc.client;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.impl.ReadStreamAdapter;
import io.vertx.grpc.common.impl.Utils;
import io.vertx.grpc.common.impl.WriteStreamAdapter;

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
      private Listener<ResponseT> listener;
      private WriteStreamAdapter<RequestT> writeAdapter = new WriteStreamAdapter<RequestT>() {
        @Override
        protected void handleReady() {
          listener.onReady();
        }
      };
      private ReadStreamAdapter<ResponseT> readAdapter = new ReadStreamAdapter<ResponseT>() {
        @Override
        protected void handleClose() {
          Status status = Status.fromCodeValue(grpcResponse.status().code);
          Metadata trailers = Utils.readMetadata(grpcResponse.trailers());
          Runnable cmd = () -> {
            listener.onClose(status, trailers);
          };
          if (exec == null) {
            cmd.run();
          } else {
            exec.execute(cmd);
          }
        }
        @Override
        protected void handleMessage(ResponseT msg) {
          if (exec == null) {
            listener.onMessage(msg);
          } else {
            exec.execute(() -> listener.onMessage(msg));
          }
        }
      };
      private GrpcClientRequest<RequestT, ResponseT> request;
      private GrpcClientResponse<RequestT, ResponseT> grpcResponse;

      @Override
      public boolean isReady() {
        return writeAdapter.isReady();
      }

      @Override
      public void start(Listener<ResponseT> responseListener, Metadata headers) {
        listener = responseListener;
        fut = client.request(server, methodDescriptor);
        fut.onComplete(ar1 -> {
          if (ar1.succeeded()) {
            request = ar1.result();
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
                readAdapter.init(grpcResponse);
              }
            });
            writeAdapter.init(request);
          } else {

          }
        });
      }

      @Override
      public void request(int numMessages) {
        readAdapter.request(numMessages);
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
          writeAdapter.write(message);
        });
      }
    };
  }

  @Override
  public String authority() {
    return null;
  }
}
