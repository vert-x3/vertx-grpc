package io.vertx.grpc.server;

public interface GrpcServiceResponse {

  void write(Object message);

  void end(Object message);

  void end();

}
