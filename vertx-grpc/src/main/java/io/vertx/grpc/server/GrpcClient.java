package io.vertx.grpc.server;

import io.grpc.MethodDescriptor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;

public class GrpcClient {

  private final Vertx vertx;
  private HttpClient client;

  public GrpcClient(Vertx vertx) {
    this.vertx = vertx;
    this.client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(false)
    );
  }

  public Future<GrpcClientRequest> request(SocketAddress server) {
    RequestOptions options = new RequestOptions()
      .setMethod(HttpMethod.POST)
      .setServer(server);
    return client.request(options)
      .map(request -> new GrpcClientRequest(request));
  }

  public <Req, Resp> Future<GrpcClientCallRequest<Req, Resp>> call(SocketAddress server, MethodDescriptor<Req, Resp> methodDesc) {
    return request(server).map(grpcRequest -> new GrpcClientCallRequest<>(grpcRequest, methodDesc));
  }

}
