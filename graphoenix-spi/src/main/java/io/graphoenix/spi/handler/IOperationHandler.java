package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IOperationHandler {

    void setupManager(IGraphqlDocumentManager manager);

    Object query(Object input) throws Exception;

    Object queryAsync(Object input) throws Exception;

    Object querySelectionsAsync(Object input) throws Exception;

    Object mutation(Object input) throws Exception;

    Object mutationAsync(Object input) throws Exception;

    Object subscription(Object input) throws Exception;
}
