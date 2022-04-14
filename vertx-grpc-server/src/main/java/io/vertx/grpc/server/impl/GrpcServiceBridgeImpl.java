package io.vertx.grpc.server.impl;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.impl.ReadStreamAdapter;
import io.vertx.grpc.common.impl.Utils;
import io.vertx.grpc.common.impl.WriteStreamAdapter;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpc.server.GrpcServiceBridge;

public class GrpcServiceBridgeImpl implements GrpcServiceBridge {

  private final ServerServiceDefinition serviceDef;

  public GrpcServiceBridgeImpl(ServerServiceDefinition serviceDef) {
    this.serviceDef = serviceDef;
  }

  @Override
  public void unbind(GrpcServer server) {
    serviceDef.getMethods().forEach(m -> unbind(server, m));
  }

  private <Req, Resp> void unbind(GrpcServer server, ServerMethodDefinition<Req, Resp> methodDef) {
    server.callHandler(methodDef.getMethodDescriptor(), null);
  }

  @Override
  public void bind(GrpcServer server) {
    serviceDef.getMethods().forEach(m -> bind(server, m));
  }

  private <Req, Resp> void bind(GrpcServer server, ServerMethodDefinition<Req, Resp> methodDef) {
    server.callHandler(methodDef.getMethodDescriptor(), req -> {
      ServerCallHandler<Req, Resp> callHandler = methodDef.getServerCallHandler();
      ServerCallImpl<Req, Resp> call = new ServerCallImpl<>(req, methodDef);
      ServerCall.Listener<Req> listener = callHandler.startCall(call, Utils.readMetadata(req.headers()));
      call.init(listener);
      req.errorHandler(error -> {
        listener.onCancel();
      });
    });
  }

  private static class ServerCallImpl<Req, Resp> extends ServerCall<Req, Resp> {

    private final GrpcServerRequest<Req, Resp> req;
    private final ServerMethodDefinition<Req, Resp> methodDef;
    private final ReadStreamAdapter<Req> readAdapter;
    private final WriteStreamAdapter<Resp> writeAdapter;
    private ServerCall.Listener<Req> listener;

    public ServerCallImpl(GrpcServerRequest<Req, Resp> req, ServerMethodDefinition<Req, Resp> methodDef) {
      this.req = req;
      this.methodDef = methodDef;
      this.readAdapter = new ReadStreamAdapter<Req>() {
        @Override
        protected void handleClose() {
          listener.onHalfClose();
        }
        @Override
        protected void handleMessage(Req msg) {
          listener.onMessage(msg);
        }
      };
      this.writeAdapter = new WriteStreamAdapter<Resp>() {
        @Override
        protected void handleReady() {
          listener.onReady();
        }
      };
    }

    void init(ServerCall.Listener<Req> listener) {
      this.listener = listener;
      readAdapter.init(req);
      writeAdapter.init(req.response());
    }

    @Override
    public boolean isReady() {
      return writeAdapter.isReady();
    }

    @Override
    public void request(int numMessages) {
      readAdapter.request(numMessages);
    }

    @Override
    public void sendHeaders(Metadata headers) {
      Utils.writeMetadata(headers, req.response().headers());
    }

    @Override
    public void sendMessage(Resp message) {
      writeAdapter.write(message);
    }

    @Override
    public void close(Status status, Metadata trailers) {
      GrpcServerResponse<Req, Resp> response = req.response();
      Utils.writeMetadata(trailers, response.trailers());
      response.status(GrpcStatus.valueOf(status.getCode().value()));
      response.end();
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public MethodDescriptor<Req, Resp> getMethodDescriptor() {
      return methodDef.getMethodDescriptor();
    }

    @Override
    public void setCompression(String compressor) {
      req.response().encoding(compressor);
    }
  }
}
