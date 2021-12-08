package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.io.IOException;

public class DocumentBuildHandler implements IBootstrapHandler {

    private final IGraphQLDocumentManager manager;
    private final DocumentBuilder documentBuilder;

    @Inject
    public DocumentBuildHandler(IGraphQLDocumentManager manager, DocumentBuilder documentBuilder) {
        this.manager = manager;
        this.documentBuilder = documentBuilder;
    }

    @Override
    public boolean execute(IPipelineContext context) throws IOException {
        manager.registerDocument(documentBuilder.buildDocument().toString());
        return false;
    }
}
