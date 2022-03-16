package io.vertx.grpc.common.impl;

import io.vertx.grpc.common.ServiceName;

public class GrpcMethodCall {

  private String path;
  private String fullMethodName;
  private ServiceName serviceName;
  private String methodName;

  public GrpcMethodCall(String path) {
    this.path = path;
  }

  public String fullMethodName() {
    if (fullMethodName == null) {
      fullMethodName = path.substring(1);
    }
    return fullMethodName;
  }

  public ServiceName serviceName() {
    if (serviceName == null) {
      int idx1 = path.lastIndexOf('.');
      int idx2 = path.lastIndexOf('/');
      serviceName = ServiceName.create(path.substring(1, idx1), path.substring(idx1 + 1, idx2));
    }
    return serviceName;
  }

  public String methodName() {
    if (methodName == null) {
      int idx = path.lastIndexOf('/');
      methodName = path.substring(idx);
    }
    return methodName;
  }
}
