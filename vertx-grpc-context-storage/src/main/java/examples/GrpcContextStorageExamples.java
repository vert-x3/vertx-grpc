package examples;

import io.grpc.Context;
import io.vertx.core.Vertx;
import io.vertx.docgen.Source;

@Source
@SuppressWarnings("unused")
public class GrpcContextStorageExamples {

  public void example(Vertx vertx) {
    Context grpcCtx1 = Context.current();

    vertx.<String>executeBlocking(prom -> {

      // Same as grpcCtx1
      Context grpcCtx2 = Context.current();

      String result = doSomething();
      prom.complete(result);

    }, ar -> {

      // Same as grpcCtx1 and grpcCtx2
      Context grpcCtx3 = Context.current();

    });
  }

  private String doSomething() {
    return null;
  }

}

