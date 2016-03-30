package io.vertx.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.testing.TestUtils;
import io.grpc.testing.integration.AbstractTransportTest;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(JUnit4.class)
public class Http2VertxTest extends AbstractTransportTest {

  private static int serverPort = TestUtils.pickUnusedPort();

  @BeforeClass
  public static void startServer() {
    try {
      startStaticServer(NettyServerBuilder.forPort(serverPort)
          .flowControlWindow(65 * 1024)
          .sslContext(GrpcSslContexts
              .forServer(TestUtils.loadCert("server1.pem"), TestUtils.loadCert("server1.key"))
              .ciphers(TestUtils.preferredTestCiphers(), SupportedCipherSuiteFilter.INSTANCE)
              .sslProvider(SslProvider.JDK)
              .build()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  protected ManagedChannel createChannel() {
    return new VertxChannelBuilder(TestUtils.testServerAddress(serverPort)).build();
/*
    try {
      return NettyChannelBuilder
          .forAddress(TestUtils.testServerAddress(serverPort))
          .sslContext(GrpcSslContexts.forClient()
              .trustManager(TestUtils.loadCert("ca.pem"))
              .ciphers(TestUtils.preferredTestCiphers(), SupportedCipherSuiteFilter.INSTANCE)
              .sslProvider(SslProvider.JDK)
              .build())
          .build();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
*/
  }

  @Override
  @Test(timeout = 10000000)
  public void emptyUnary() throws Exception {
    super.emptyUnary();
  }
}
