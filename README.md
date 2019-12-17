[![Build Status](https://travis-ci.org/vert-x3/vertx-grpc.svg?branch=master)](https://travis-ci.org/vert-x3/vertx-grpc)

# Vert.x gRPC

Currently for Vert.x 3.4.0-SNAPSHOT

- [Documentation](src/main/asciidoc/java/index.adoc)
- [Vert.x gRPC Java Compiler](https://github.com/vert-x3/vertx-grpc-java-compiler)

Supports:

- client and server
- server scaling
- ssl configuration with options
- auto close in Verticle

Todo:

- contribute support to grpc-java Netty implementation to provide async start/shutdown
- worker integration ?

## Plugin installation

To use vertx-grpc-protoc-plugin with the protobuf-maven-plugin, add a [custom protoc plugin configuration section](https://www.xolstice.org/protobuf-maven-plugin/examples/protoc-plugin.html).

```
<protocPlugins>
    <protocPlugin>
        <id>vertx-grpc-protoc-plugin</id>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-grpc-protoc-plugin</artifactId>
        <version>[VERSION]</version>
        <mainClass>io.vertx.grpc.protoc.plugin.VertxGrpcGenerator</mainClass>
    </protocPlugin>
</protocPlugins>
``` 

And add the [vertx-grpc](https://github.com/vert-x3/vertx-grpc) dependency:

```
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-grpc</artifactId>
  <version>[VERSION]</version>
</dependency>
```
