package io.graphoenix.meta.spi;

import io.graphoenix.meta.OperationType;
import io.graphoenix.meta.antlr.IGraphqlDocumentManager;

public interface IGraphQLOperationPipeline<I, O> {

    <H extends IGraphQLOperationHandler<?, ?>> IGraphQLOperationPipeline<I, O> push(Class<H> handleClass);

    IGraphQLOperationPipeline<I, O> build();

    IGraphqlDocumentManager getManager();

    OperationType getOperationType(I request);

    O order(I request);
}
