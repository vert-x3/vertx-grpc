package io.vertx.grpc.server.stub;

import io.grpc.BindableService;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.stub.impl.GrpcStubImpl;

/**
 * Bridge a gRPC service with a {@link GrpcServer}.
 */
public interface GrpcStub {

  /**
   * Create a stub for a given {@code service}.
   *
   * @param service the service
   * @return the stub
   */
  static GrpcStub stub(BindableService service) {
    return new GrpcStubImpl(service.bindService());
  }

  /**
   * Bind all the methods of the stub to the @{code server}.
   *
   * @param server the server to bind to
   */
  void bind(GrpcServer server);

  /**
   * Unbind all the methods of the stub from the @{code server}.
   *
   * @param server the server to unbind from
   */
  void unbind(GrpcServer server);

}
