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
package io.vertx.grpc.server;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.streams.WriteStream;
import io.vertx.grpc.common.GrpcStatus;

@VertxGen
public interface GrpcServerResponse<Req, Resp> extends WriteStream<Resp> {

  /**
   * Set the grpc status response
   *
   * @param status the status
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  GrpcServerResponse<Req, Resp> status(GrpcStatus status);


  /**
   * Reset the stream.
   *
   * This is an HTTP/2 operation.
   */
  @Fluent
  GrpcServerResponse<Req, Resp> reset();

  @Fluent
  GrpcServerResponse<Req, Resp> encoding(String encoding);

  /**
   * @return the {@link MultiMap} to write metadata headers
   */
  MultiMap headers();

  /**
   * @return the {@link MultiMap} to write metadata trailers
   */
  MultiMap trailers();

  @Override
  GrpcServerResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler);

  @Override
  GrpcServerResponse<Req, Resp> setWriteQueueMaxSize(int maxSize);

  @Override
  GrpcServerResponse<Req, Resp> drainHandler(@Nullable Handler<Void> handler);
}
