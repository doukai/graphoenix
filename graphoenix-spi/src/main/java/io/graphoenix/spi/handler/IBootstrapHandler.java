package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

public interface IBootstrapHandler {

    Object transform(IGraphQLDocumentManager manager, Object object) throws Exception;
}
