package io.vertx.grpc.server;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.vertx.grpc.server.impl.GrpcServiceBridgeImpl;

/**
 * Bridge a gRPC service with a {@link GrpcServer}.
 */
public interface GrpcServiceBridge {

  /**
   * Create a stub for a given {@code service}.
   *
   * @param service the service
   * @return the stub
   */
  static GrpcServiceBridge bridge(ServerServiceDefinition service) {
    return new GrpcServiceBridgeImpl(service);
  }

  /**
   * Create a stub for a given {@code service}.
   *
   * @param service the service
   * @return the stub
   */
  static GrpcServiceBridge bridge(BindableService service) {
    return bridge(service.bindService());
  }

  /**
   * Bind all service methods to the @{code server}.
   *
   * @param server the server to bind to
   */
  void bind(GrpcServer server);

  /**
   * Unbind all service methods from the @{code server}.
   *
   * @param server the server to unbind from
   */
  void unbind(GrpcServer server);

}
