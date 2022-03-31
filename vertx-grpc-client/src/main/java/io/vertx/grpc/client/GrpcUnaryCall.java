package io.vertx.grpc.client;

import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;

public interface GrpcUnaryCall<Req, Resp> {

  Future<Resp> call(SocketAddress server, Req msg);

}
