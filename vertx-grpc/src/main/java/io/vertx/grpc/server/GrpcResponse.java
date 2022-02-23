package io.vertx.grpc.server;

public interface GrpcResponse {

  void write(Object message);

  void end(Object message);

  void end();

}
