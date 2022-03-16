package io.vertx.grpc.common.impl;

import io.vertx.grpc.common.ServiceName;

public class ServiceNameImpl implements ServiceName {

  private String name;
  private String packageName;
  private String fullyQualifiedName;

  public ServiceNameImpl(String packageName, String name) {
    this.name = name;
    this.packageName = packageName;
  }

  public ServiceNameImpl(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
  }

  @Override
  public String name() {
    if (name == null) {
      int idx = fullyQualifiedName.lastIndexOf('.');
      name = fullyQualifiedName.substring(idx);
    }
    return name;
  }

  @Override
  public String packageName() {
    if (packageName == null) {
      int idx = fullyQualifiedName.lastIndexOf('.');
      packageName = fullyQualifiedName.substring(0, idx);
    }
    return packageName;
  }

  @Override
  public String fullyQualifiedName() {
    if (fullyQualifiedName == null) {
      fullyQualifiedName = packageName + '.' + name;
    }
    return packageName;
  }

  @Override
  public String pathOf(String methodName) {
    if (fullyQualifiedName != null) {
      return '/' + fullyQualifiedName + '/' + methodName;
    } else {
      return '/' + packageName + '.' + name + '/' + methodName;
    }
  }
}
