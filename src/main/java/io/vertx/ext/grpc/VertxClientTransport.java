package io.vertx.ext.grpc;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.internal.ClientStream;
import io.grpc.internal.ManagedClientTransport;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxClientTransport implements ManagedClientTransport {

  private final SocketAddress socketAddress;
  private final String authority;
  private final Vertx vertx = Vertx.vertx();
  private final HttpClient client = vertx.createHttpClient(new HttpClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setVerifyHost(false)
      .setUseAlpn(true)
      .setTrustAll(true));
  private Listener listener;

  public VertxClientTransport(SocketAddress socketAddress, String authority) {
    this.socketAddress = socketAddress;
    this.authority = authority;
  }

  @Override
  public void start(Listener listener) {
    this.listener = listener;
  }

  @Override
  public void shutdown() {
    vertx.close();
  }

  @Override
  public ClientStream newStream(MethodDescriptor<?, ?> method, Metadata headers) {
    return new VertxClientStream(vertx.getOrCreateContext(), client, socketAddress, authority, method, headers);
  }

  @Override
  public void ping(PingCallback callback, Executor executor) {
    throw new UnsupportedOperationException();
  }
}
