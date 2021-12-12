package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.io.IOException;

public class DocumentBuildHandler implements IBootstrapHandler {

    private final DocumentBuilder documentBuilder;

    @Inject
    public DocumentBuildHandler( DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    @Override
    public boolean execute(IPipelineContext context) throws IOException {
        documentBuilder.buildManager();
        return false;
    }
}
