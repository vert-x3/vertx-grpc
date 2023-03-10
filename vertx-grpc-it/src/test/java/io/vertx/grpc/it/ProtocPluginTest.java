/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.grpc.it;

import com.google.protobuf.ByteString;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.VertxTestServiceGrpcClient;
import io.grpc.testing.integration.VertxTestServiceGrpcServer;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProtocPluginTest extends ProxyTestBase {

  @Test
  public void testUnary(TestContext should) throws IOException {
    port = getFreePort();

    // Create gRPC Server
    VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
      .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
        @Override
        public void unaryCall(Messages.SimpleRequest request, Promise<Messages.SimpleResponse> response) {
          response.complete(Messages.SimpleResponse.newBuilder()
            .setUsername("FooBar")
            .build());
        }
      });
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(server.getGrpcServer())
      .listen(port)
      .onFailure(should::fail);

    // Create gRPC Client
    VertxTestServiceGrpcClient client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));

    Async test = should.async();
    client.unaryCall(Messages.SimpleRequest.newBuilder()
        .setFillUsername(true)
        .build())
      .onSuccess(reply -> should.assertEquals("FooBar", reply.getUsername()))
      .onSuccess(reply -> test.complete())
      .onFailure(should::fail);
  }

  @Test
  public void testManyUnary(TestContext should) throws IOException {
    int port = getFreePort();

    // Create gRPC Server
    VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
      .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
        @Override
        public void streamingInputCall(ReadStream<Messages.StreamingInputCallRequest> request, Promise<Messages.StreamingInputCallResponse> response) {
          List<Messages.StreamingInputCallRequest> list = new ArrayList<>();
          request.handler(list::add);
          request.endHandler($ -> {
            Messages.StreamingInputCallResponse resp = Messages.StreamingInputCallResponse.newBuilder()
              .setAggregatedPayloadSize(list.size())
              .build();
            response.complete(resp);
          });
        }
      });
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(server.getGrpcServer())
      .listen(port)
      .onFailure(should::fail);

    // Create gRPC Client
    VertxTestServiceGrpcClient client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));

    Async test = should.async();
    client.streamingInputCall(req -> {
        req.write(Messages.StreamingInputCallRequest.newBuilder()
          .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-1", StandardCharsets.UTF_8)).build())
          .build());
        req.write(Messages.StreamingInputCallRequest.newBuilder()
          .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-2", StandardCharsets.UTF_8)).build())
          .build());
        req.end();
      })
      .onSuccess(reply -> should.assertEquals(2, reply.getAggregatedPayloadSize()))
      .onSuccess(reply -> test.complete())
      .onFailure(should::fail);
  }

  @Test
  public void testUnaryMany(TestContext should) throws IOException {
    int port = getFreePort();

    // Create gRPC Server
    VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
      .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
        @Override
        public void streamingOutputCall(Messages.StreamingOutputCallRequest request, WriteStream<Messages.StreamingOutputCallResponse> response) {
          response.write(Messages.StreamingOutputCallResponse.newBuilder()
            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
            .build());
          response.write(Messages.StreamingOutputCallResponse.newBuilder()
            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
            .build());
          response.end();
        };
      });
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(server.getGrpcServer())
      .listen(port)
      .onFailure(should::fail);

    // Create gRPC Client
    VertxTestServiceGrpcClient client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));

    Async test = should.async();
    Messages.StreamingOutputCallRequest request = Messages.StreamingOutputCallRequest.newBuilder()
      .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest", StandardCharsets.UTF_8)).build())
      .build();
    client.streamingOutputCall(request)
      .onSuccess(response -> {
        List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
        response.handler(list::add);
        response.endHandler($ -> {
          should.assertEquals(2, list.size());
          test.complete();
        });
        response.exceptionHandler(should::fail);
      });
  }

  @Test
  public void testmanyMany(TestContext should) throws IOException {
    int port = getFreePort();

    // Create gRPC Server
    VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
      .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
        @Override
        public void fullDuplexCall(ReadStream<Messages.StreamingOutputCallRequest> request, WriteStream<Messages.StreamingOutputCallResponse> response) {
          request.endHandler($ -> {
            response.write(Messages.StreamingOutputCallResponse.newBuilder()
              .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
              .build());
            response.write(Messages.StreamingOutputCallResponse.newBuilder()
              .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
              .build());
            response.end();
          });
        };
      });
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(server.getGrpcServer())
      .listen(port)
      .onFailure(should::fail);

    // Create gRPC Client
    VertxTestServiceGrpcClient client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));

    Async test = should.async();
    client.fullDuplexCall(req -> {
        req.write(Messages.StreamingOutputCallRequest.newBuilder()
          .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-1", StandardCharsets.UTF_8)).build())
          .build());
        req.write(Messages.StreamingOutputCallRequest.newBuilder()
          .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-2", StandardCharsets.UTF_8)).build())
          .build());
        req.end();
      })
      .onSuccess(response -> {
        List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
        response.handler(list::add);
        response.endHandler($ -> {
          should.assertEquals(2, list.size());
          test.complete();
        });
        response.exceptionHandler(should::fail);
      });
  }

  private static Integer getFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
