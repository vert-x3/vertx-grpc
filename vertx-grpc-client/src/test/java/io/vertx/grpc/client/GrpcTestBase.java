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
package io.vertx.grpc.client;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public abstract class GrpcTestBase {

  /* The port on which the server should run */
  Vertx vertx;
  int port;
  protected Server server;

  @Before
  public void setUp() {
    port = 8080;
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext should) {
    Server s = server;
    if (s != null) {
      server = null;
      s.shutdown();
    }
    vertx.close(should.asyncAssertSuccess());
  }

  void startServer(BindableService service) throws IOException {
    startServer(service, ServerBuilder.forPort(port));
  }

  void startServer(BindableService service, ServerBuilder builder) throws IOException {
    server = builder
        .addService(service)
        .build()
        .start();
  }


  void startServer(ServerServiceDefinition service) throws IOException {
    startServer(service, ServerBuilder.forPort(port));
  }

  void startServer(ServerServiceDefinition service, ServerBuilder builder) throws IOException {
    server = builder
      .addService(service)
      .build()
      .start();
  }
}
