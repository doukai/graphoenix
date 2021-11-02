package io.graphoenix.spi.handler.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IOperationHandler<I, O> {

    void setupManager(IGraphqlDocumentManager manager);

    O query(I input) throws Exception;

    O mutation(I input) throws Exception;

    O subscription(I input) throws Exception;
}
