[![Build Status](https://vertx.ci.cloudbees.com/buildStatus/icon?job=vert.x3-grpc)](https://vertx.ci.cloudbees.com/view/vert.x-3/job/vert.x3-grpc/)

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

- more options ?
- write doc & examples
- contribute support to grpc-java Netty implementation to provide async start/shutdown
- on the client side check if the managed channel auto reconnects
- worker integration ?
