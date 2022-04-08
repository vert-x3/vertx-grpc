package io.vertx.grpc.server.stub.impl;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.impl.Utils;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.stub.GrpcStub;

public class GrpcStubImpl implements GrpcStub {

  private final ServerServiceDefinition serviceDef;

  public GrpcStubImpl(ServerServiceDefinition serviceDef) {
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
      ServerCall<Req, Resp> call = new ServerCall<Req, Resp>() {
        @Override
        public void request(int numMessages) {
          // ?
        }
        @Override
        public void sendHeaders(Metadata headers) {
          Utils.writeMetadata(headers, req.response().headers());
        }
        @Override
        public void sendMessage(Resp message) {
          req.response().write(message);
        }
        @Override
        public void close(Status status, Metadata trailers) {
          Utils.writeMetadata(trailers, req.response().trailers());
          req.response().status(GrpcStatus.valueOf(status.getCode().value()));
          req.response().end();
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
      };
      ServerCall.Listener<Req> listener = callHandler.startCall(call, Utils.readMetadata(req.headers()));
      req.messageHandler(msg -> {
        listener.onMessage(msg);
      });
      req.errorHandler(error -> {
        listener.onCancel();
      });
      req.endHandler(v -> {
        listener.onHalfClose();
      });
    });
  }
}
