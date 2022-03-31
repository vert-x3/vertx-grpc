package io.vertx.grpc.client;

import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;

/**
 *
 */
public interface GrpcClientStreamingCall<Req, Resp> {

  Future<Resp> call(SocketAddress server, ReadStream<Req> msg);

}
