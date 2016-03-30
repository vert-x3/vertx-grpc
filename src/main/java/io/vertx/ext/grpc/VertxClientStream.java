package io.vertx.ext.grpc;

import io.grpc.Compressor;
import io.grpc.Decompressor;
import io.grpc.Drainable;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.internal.AbstractClientStream;
import io.grpc.internal.ClientStream;
import io.grpc.internal.ClientStreamListener;
import io.grpc.internal.Http2ClientStream;
import io.grpc.internal.TransportFrameUtil;
import io.grpc.internal.WritableBuffer;
import io.grpc.internal.WritableBufferAllocator;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.util.AsciiString;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxClientStream extends Http2ClientStream {

  private static byte[][] convertHeadersToArray(MultiMap http2Headers) {
    // The Netty AsciiString class is really just a wrapper around a byte[] and supports
    // arbitrary binary data, not just ASCII.
    byte[][] headerValues = new byte[http2Headers.size() * 2][];
    int i = 0;
    for (Map.Entry<String, String> entry : http2Headers) {
      headerValues[i++] = bytes(entry.getKey());
      headerValues[i++] = bytes(entry.getValue());
    }
    return TransportFrameUtil.toRawSerializedHeaders(headerValues);
  }

  private static byte[] bytes(CharSequence seq) {
    if (seq instanceof AsciiString) {
      // Fast path - no copy.
      return ((AsciiString) seq).array();
    }
    // Slow path - copy.
    return seq.toString().getBytes(UTF_8);
  }

  private final Context context;
  private final HttpClient client;
  private final InetSocketAddress socketAddress;
  private final String authority;
  private final MethodDescriptor<?, ?> method;
  private final Metadata headers;
  private HttpClientRequest request;
  private Buffer body = Buffer.buffer();
  private Metadata responseHeaders;

  public VertxClientStream(Context context, HttpClient client, SocketAddress socketAddress, String authority, MethodDescriptor<?, ?> method, Metadata headers) {
    super(VertxBuffer::new, DEFAULT_MAX_MESSAGE_SIZE);
    this.context = context;
    this.client = client;
    this.socketAddress = (InetSocketAddress) socketAddress;
    this.authority = authority;
    this.method = method;
    this.headers = headers;
  }

  private void handleResponse(HttpClientResponse response) {
    responseHeaders = new Metadata(convertHeadersToArray(response.headers()));
    response.handler(buff -> {
      if (responseHeaders != null) {
        transportHeadersReceived(responseHeaders);
        responseHeaders = null;
      }
      transportDataReceived(new NettyReadableBuffer(buff.getByteBuf()), false);
      body.appendBuffer(buff);
    });
    response.exceptionHandler(err -> {
      err.printStackTrace();
    });
    response.endHandler(v -> {
      Metadata trailers;
      if (responseHeaders != null) {
        // Headers are actually trailers
        trailers = responseHeaders;
        responseHeaders = null;
      } else {
        trailers = new Metadata(convertHeadersToArray(response.trailers()));
      }
      transportTrailersReceived(trailers);
    });
  }

  @Override
  public void start(ClientStreamListener listener) {
    super.start(listener);
    String path = "/" + method.getFullMethodName();
    request = client.post(socketAddress.getPort(), socketAddress.getAddress().getHostAddress(), path, this::handleResponse);
    request.setChunked(true);
    request.exceptionHandler(err -> {
      err.printStackTrace();
    });
    byte[][] serializedHeaders = TransportFrameUtil.toHttp2Headers(headers);
    for (int i = 0; i < serializedHeaders.length; i += 2) {
      request.putHeader(new String(serializedHeaders[i]), new String(serializedHeaders[i + 1]));
    }
    request.setHost(authority);
    request.putHeader("content-type", "application/grpc");
    request.putHeader("te", "trailers");
  }

  @Override
  public void setAuthority(String authority) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void request(int numMessages) {
    context.runOnContext(v -> {
      requestMessagesFromDeframer(numMessages);
    });
  }

  @Override
  protected void sendFrame(WritableBuffer frame, boolean endOfStream, boolean flush) {
    if (frame != null) {
      Buffer buffer = ((VertxBuffer)frame).buffer;
      request.write(buffer);
    }
    if (endOfStream) {
      request.end();
    }
  }

  @Override
  protected void sendCancel(Status reason) {
    request.reset(Http2Error.CANCEL.code());
    transportReportStatus(reason, true, new Metadata());
  }

  @Nullable
  @Override
  public Integer id() {
    int id = request.streamId();
    if (id == -1) {
      return null;
    }
    return id;
  }

  @Override
  protected void returnProcessedBytes(int processedBytes) {
  }
}
