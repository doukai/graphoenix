package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

public interface IOperationHandler {

    void setupManager(IGraphQLDocumentManager manager);

    Object query(Object input) throws Exception;

    Object queryAsync(Object input) throws Exception;

    Object querySelectionsAsync(Object input) throws Exception;

    Object mutation(Object input) throws Exception;

    Object mutationAsync(Object input) throws Exception;

    Object subscription(Object input) throws Exception;
}
