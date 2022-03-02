package io.vertx.grpc.server;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

public enum GrpcStatus {

  OK(0),

  CANCELLED(1),

  UNKNOWN(2),

  INVALID_ARGUMENT(3),

  DEADLINE_EXCEEDED(4),

  NOT_FOUND(5),

  ALREADY_EXISTS(6),

  PERMISSION_DENIED(7),

  RESOURCE_EXHAUSTED(8),

  FAILED_PRECONDITION(9),

  ABORTED(10),

  OUT_OF_RANGE(11),

  UNIMPLEMENTED(12),

  INTERNAL(13),

  UNAVAILABLE(14),

  DATA_LOSS(15),

  UNAUTHENTICATED(16);

  private static final IntObjectMap<GrpcStatus> codeMap = new IntObjectHashMap<>();

  public static GrpcStatus valueOf(int code) {
    return codeMap.get(code);
  }

  static {
    for (GrpcStatus status : values()) {
      codeMap.put(status.code, status);
    }
  }

  public final int code;
  private final String string;

  GrpcStatus(int code) {
    this.code = code;
    this.string = Integer.toString(code);
  }


  @Override
  public String toString() {
    return string;
  }
}
