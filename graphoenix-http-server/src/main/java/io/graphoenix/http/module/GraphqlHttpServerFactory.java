package io.graphoenix.http.module;

import dagger.assisted.AssistedFactory;
import io.graphoenix.core.pipeline.operation.OperationRouter;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.spi.handler.BootstrapHandler;

@AssistedFactory
public interface GraphqlHttpServerFactory {
    GraphqlHttpServer create(OperationRouter operationRouter, BootstrapHandler bootstrapHandler);
}
