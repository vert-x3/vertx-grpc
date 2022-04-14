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
package io.vertx.grpc.common;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.grpc.common.impl.ServiceNameImpl;

@VertxGen
public interface ServiceName {

  static ServiceName create(String fqn) {
    return new ServiceNameImpl(fqn);
  }

  static ServiceName create(String packageName, String name) {
    return new ServiceNameImpl(packageName, name);
  }

  @CacheReturn
  String name();

  @CacheReturn
  String packageName();

  @CacheReturn
  String fullyQualifiedName();

  String pathOf(String methodName);

}
