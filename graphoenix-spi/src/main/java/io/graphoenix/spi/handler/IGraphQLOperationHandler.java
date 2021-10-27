package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IGraphQLOperationHandler<I, O> {

    void assign(IGraphqlDocumentManager manager);

    O query(I object);

    O mutation(I object);

    O subscription(I object);
}
