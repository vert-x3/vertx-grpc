package io.vertx.grpc.common.impl;

import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import io.netty.util.AsciiString;
import io.vertx.core.MultiMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Utils {

  public static void writeMetadata(Metadata metadata, MultiMap mmap) {
    byte[][] t = InternalMetadata.serialize(metadata);
    for (int i = 0;i < t.length;i+=2) {
      mmap.add(new AsciiString(t[i], false), new AsciiString(t[i + 1], false));
    }
  }

  public static Metadata readMetadata(MultiMap headers) {
    byte[][] abc = new byte[headers.size() * 2][];
    int idx = 0;
    for (Map.Entry<String, String> entry : headers) {
      abc[idx++] = entry.getKey().getBytes(StandardCharsets.UTF_8);
      abc[idx++] = entry.getValue().getBytes(StandardCharsets.UTF_8);
    }
    return InternalMetadata.newMetadata(abc);
  }
}
