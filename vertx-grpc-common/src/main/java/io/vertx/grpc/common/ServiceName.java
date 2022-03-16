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
