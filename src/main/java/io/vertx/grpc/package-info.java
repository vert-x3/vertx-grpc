/**
 * = Vert.x gRPC
 *
 * The best description of gRPC can be seen at wikipedia.
 *
 * [quote, wikipedia, wikipedia]
 * ____
 * gRPC is an open source remote procedure call (RPC) system initially developed at Google. It uses HTTP/2 for
 * transport, Protocol Buffers as the interface description language, and provides features such as authentication,
 * bidirectional streaming and flow control, blocking or nonblocking bindings, and cancellation and timeouts. It
 * generates cross-platform client and server bindings for many languages.
 * ____
 *
 * Vert.x gRPC is a module that will align the programming style of Google gRPC with Vert.x style. As a user of this
 * module you will be more familiar with the code style using Vert.x Streams and Futures while benefiting from all the
 * benefits of gRPC.
 *
 * For more information related to gRPC please consult the official documentation site http://www.grpc.io/.
 *
 * == gRPC types
 *
 * With gRPC you benefit from HTTP/2 which means that you will have asynchronous streaming support which means that your
 * Remote Procedure Calls can have the following characteristics:
 *
 * * Client streams request objects while Server replies with a single response object
 * * Client streams request objects while Server replies with a stream of response objects
 * * Client sends a single request object while Server replies with a single response object
 * * Client sends a single request object while Server replies with a stream of response objects
 *
 * While to the untrained eye this might not look to different from other HTTP based RPC approaches you should be aware
 * that with HTTP/2 your requests do not need to complete before the responses start to arrive. This means that your
 * communication channel is full duplex. Being full duplex allows you to reduce the response latency and make more
 * response application.
 *
 * == A simple Hello World
 *
 * In order to start with your first hello world example, one needs to define the protocol. gRPC requires you to define
 * this protocol using the `protobuffer` format.
 *
 * [source,proto]
 * ----
 * syntax = "proto3";
 *
 * option java_multiple_files = true;
 * option java_package = "examples";
 * option java_outer_classname = "HelloWorldProto";
 * package helloworld;
 *
 * // The greeting service definition.
 * service Greeter {
 *   // Sends a greeting
 *   rpc SayHello (HelloRequest) returns (HelloReply) {}
 * }
 *
 * // The request message containing the user's name.
 * message HelloRequest {
 *   string name = 1;
 * }
 *
 * // The response message containing the greetings
 * message HelloReply {
 *   string message = 1;
 * }
 * ----
 *
 * This is a very simple example showing the single request, single response mode.
 *
 * === Compile the RPC definition
 *
 * Using the definition above we need to compile it.
 *
 * You can compile the proto file using the `protoc` compiler if you https://github.com/google/protobuf/tree/master/java#installation---without-maven[like]
 * or you can integrate it in your build.
 *
 * If you're using Apache Maven you need to add the plugin:
 *
 * [source,xml]
 * ----
 *  <plugin>
 *   <groupId>org.xolstice.maven.plugins</groupId>
 *   <artifactId>protobuf-maven-plugin</artifactId>
 *   <version>0.5.0</version>
 *   <configuration>
 *     <protocArtifact>com.google.protobuf:protoc:3.3.0:exe:${os.detected.classifier}</protocArtifact>
 *     <pluginId>grpc-java</pluginId>
 *     <pluginArtifact>io.vertx:protoc-gen-grpc-java:${vertx.grpc.version}:${os.detected.classifier}</pluginArtifact>
 *   </configuration>
 *   <executions>
 *     <execution>
 *       <id>compile</id>
 *       <configuration>
 *         <outputDirectory>${project.basedir}/src/main/java</outputDirectory>
 *         <clearOutputDirectory>false</clearOutputDirectory>
 *       </configuration>
 *       <goals>
 *         <goal>compile</goal>
 *         <goal>compile-custom</goal>
 *       </goals>
 *     </execution>
 *     <execution>
 *       <id>test-compile</id>
 *       <goals>
 *         <goal>test-compile</goal>
 *         <goal>test-compile-custom</goal>
 *       </goals>
 *     </execution>
 *   </executions>
 * </plugin>
 * ----
 *
 * The `${os.detected.classifier}` property is used to make the build OS independant, on OSX it is replaced
 * by _osx-x86_64_ and so on. To use it you need to add the os-maven-plugin[https://github.com/trustin/os-maven-plugin]
 * in the `build` section of your `pom.xml`:
 *
 * [source,xml]
 * ----
 * <build>
 *   ...
 *   <extensions>
 *     <extension>
 *       <groupId>kr.motd.maven</groupId>
 *       <artifactId>os-maven-plugin</artifactId>
 *       <version>1.5.0.Final</version>
 *     </extension>
 *   </extensions>
 *   ...
 * </build>
 * ----
 *
 * This plugin will compile your proto files under `src/main/proto` and make them available to your project.
 *
 * If you're using Gradle you need to add the plugin:
 *
 * [source,groovy]
 * ----
 * ...
 * apply plugin: 'com.google.protobuf'
 * ...
 * buildscript {
 *   ...
 *   dependencies {
 *     // ASSUMES GRADLE 2.12 OR HIGHER. Use plugin version 0.7.5 with earlier gradle versions
 *     classpath 'com.google.protobuf:protobuf-gradle-plugin:0.8.1'
 *   }
 * }
 * ...
 * protobuf {
 *   protoc {
 *     artifact = 'com.google.protobuf:protoc:3.3.0'
 *   }
 *   plugins {
 *   grpc {
 *     artifact = "io.vertx:protoc-gen-grpc-java:${vertx.grpc.version}"
 *   }
 * }
 *   generateProtoTasks {
 *     all()*.plugins {
 *       grpc
 *     }
 *   }
 * }
 * ----
 *
 * This plugin will compile your proto files under `build/generated/source/proto/main` and make them available to your project.
 *
 * === gRPC Server
 *
 * Now you should have your RPC base code setup it is time to implement your server. As you should recall from above we
 * described that our server should implement a `sayHello` method that receives a `HelloRequest` objects and returns a
 * `HelloReply` object. So you can implement it as:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#simpleServer}
 * ----
 *
 * Once you're happy with it you can then make your service available on a server. Vert.x makes the creation of a server
 * quite simple all you need to add is:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#startServer}
 * ----
 *
 * ==== SSL configuration
 *
 * The previous example was simple but your RPC is not secure. In order to make it secure we should enable SSL/TLS:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#sslServer}
 * ----
 *
 * Congratulations you just completed your first gRPC server.
 *
 * IMPORTANT: since gRPC uses HTTP/2 transport, SSL/TLS setup requires the
 * https://wikipedia.org/wiki/Application-Layer_Protocol_Negotiation[Application-Layer Protocol Negotiation]
 * in your server
 *
 * === gRPC Client
 *
 * A server without a client is of no use, so lets create a client. In order to do this some steps overlap with the
 * server. First we need to have the RPC definition, which should already done otherwise there would be no server and
 * the same definition should have been compiled.
 *
 * Note that the compiler will always generate both the base server and a client stub so if you already compiled once
 * you do not need to re-compile it again.
 *
 * Every client stub will always require a communication channel to a server so first we need to create a gRPC channel:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#connectClient}
 * ----
 *
 * Once the stub is created we can communicate with our server, this time it is easier since the stub already provides
 * the correct method definition and parameter types:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#simpleClient}
 * ----
 *
 * ==== SSL configuration
 *
 * If you enabled SSL previously your client will also require SSL, in order to do this we need to configure the channel:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#sslClient}
 * ----
 *
 * IMPORTANT: since gRPC uses HTTP/2 transport, SSL/TLS setup requires the
 * https://wikipedia.org/wiki/Application-Layer_Protocol_Negotiation[Application-Layer Protocol Negotiation]
 * in your client
 *
 * == Advanced configuration
 *
 * Until now all gRPC examples where using sensible defaults but there is more, if you need to have full control over
 * the server configuration you should refer to the documentation: {@link io.vertx.grpc.VertxServerBuilder}, or if you
 * need to control your client channel {@link io.vertx.grpc.VertxChannelBuilder}. Vert.x gRPC extends the grpc-java
 * project (Netty transport) and therefore reading its http://www.grpc.io/grpc-java/javadoc/[documentation] is
 * recommended.
 */
@Document(fileName = "index.adoc")
package io.vertx.grpc;

import io.vertx.docgen.Document;
