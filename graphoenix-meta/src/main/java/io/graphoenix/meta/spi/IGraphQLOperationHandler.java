package io.graphoenix.meta.spi;

import io.graphoenix.meta.OperationType;
import io.graphoenix.meta.antlr.IGraphqlDocumentManager;

public interface IGraphQLOperationHandler<I, O> {

    void assign(IGraphqlDocumentManager manager);

    O query(I object);

    O mutation(I object);

    O subscription(I object);
}
