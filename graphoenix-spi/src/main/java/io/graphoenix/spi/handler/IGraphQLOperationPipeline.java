package io.graphoenix.spi.handler;

import io.graphoenix.spi.OperationType;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IGraphQLOperationPipeline<I, O> {

    @SuppressWarnings("rawtypes")
    <H extends IGraphQLOperationHandler> IGraphQLOperationPipeline<I, O> push(Class<H> handlerClass);

    IGraphQLOperationPipeline<I, O> build();

    IGraphqlDocumentManager getManager();

    OperationType getOperationType(I request);

    O order(I request);
}
