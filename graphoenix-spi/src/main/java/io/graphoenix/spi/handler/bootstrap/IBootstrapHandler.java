package io.graphoenix.spi.handler.bootstrap;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

public interface IBootstrapHandler<I, O> {

    O transform(IGraphqlDocumentManager manager, I object) throws Exception;

    void process(IGraphqlDocumentManager manager) throws Exception;
}
