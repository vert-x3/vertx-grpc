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

import io.vertx.codegen.annotations.VertxGen;

/**
 * gRPC error.
 */
@VertxGen
public enum GrpcError {

  INTERNAL,

  UNAVAILABLE,

  CANCELLED,

  RESOURCE_EXHAUSTED,

  PERMISSION_DENIED;

  /**
   * Map the HTTP/2 code to the gRPC error.
   * @param code the HTTP/2 code
   * @return the gRPC error or {@code null} when none applies
   */
  public static GrpcError mapHttp2ErrorCode(long code) {
    switch ((int)code) {
      case 0:
        // NO_ERROR
      case 1:
        // PROTOCOL_ERROR
      case 2:
        // INTERNAL_ERROR
      case 3:
        // FLOW_CONTROL_ERROR
      case 4:
        // FRAME_SIZE_ERROR
      case 6:
        // FRAME_SIZE_ERROR
      case 7:
        // REFUSED_STREAM
      case 9:
        // COMPRESSION_ERROR
        return GrpcError.INTERNAL;
      case 10:
        // CONNECT_ERROR
      case 8:
        // CANCEL
        return GrpcError.CANCELLED;
      case 11:
        // ENHANCE_YOUR_CALM
        return GrpcError.RESOURCE_EXHAUSTED;
      case 12:
        // INADEQUATE_SECURITY
        return GrpcError.PERMISSION_DENIED;
      default:
        // STREAM_CLOSED;
        // HTTP_1_1_REQUIRED
        return null;
    }
  }
}
