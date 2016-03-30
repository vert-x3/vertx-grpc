package io.vertx.ext.grpc;

import io.grpc.internal.AbstractManagedChannelImplBuilder;
import io.grpc.internal.ClientTransportFactory;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.ManagedClientTransport;
import io.grpc.internal.ReferenceCounted;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxChannelBuilder extends AbstractManagedChannelImplBuilder<VertxChannelBuilder> {

  private static String getAuthorityFromAddress(SocketAddress address) {
    if (address instanceof InetSocketAddress) {
      InetSocketAddress inetAddress = (InetSocketAddress) address;
      return GrpcUtil.authorityFromHostAndPort(inetAddress.getHostString(), inetAddress.getPort());
    } else {
      return address.toString();
    }
  }

  public VertxChannelBuilder(String target) {
    super(target);
  }

  public VertxChannelBuilder(SocketAddress directServerAddress) {
    super(directServerAddress, getAuthorityFromAddress(directServerAddress));
  }

  @Override
  protected ClientTransportFactory buildTransportFactory() {
    return new ClientTransportFactory() {

      private AtomicInteger count = new AtomicInteger();

      @Override
      public ManagedClientTransport newClientTransport(SocketAddress socketAddress, String authority) {
        return new VertxClientTransport(socketAddress, authority);
      }

      @Override
      public int referenceCount() {
        return count.get();
      }

      @Override
      public ReferenceCounted retain() {
        count.getAndIncrement();
        return this;
      }

      @Override
      public ReferenceCounted release() {
        count.getAndDecrement();
        return this;
      }
    };
  }

  @Override
  public VertxChannelBuilder usePlaintext(boolean b) {
    throw new UnsupportedOperationException();
  }
}
