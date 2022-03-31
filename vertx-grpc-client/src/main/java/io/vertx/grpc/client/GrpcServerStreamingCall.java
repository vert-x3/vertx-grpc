package io.vertx.grpc.client;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;

import java.util.stream.Collector;

/**
 *
 */
public interface GrpcServerStreamingCall<Req, Resp> {

  Future<ReadStream<Resp>> call(SocketAddress server, Req msg);

}
