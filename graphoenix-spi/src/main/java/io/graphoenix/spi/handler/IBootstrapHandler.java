package io.graphoenix.spi.handler;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IBootstrapHandler {

    Object transform(IGraphqlDocumentManager manager, Object object) throws Exception;

    void process(IGraphqlDocumentManager manager) throws Exception;
}
