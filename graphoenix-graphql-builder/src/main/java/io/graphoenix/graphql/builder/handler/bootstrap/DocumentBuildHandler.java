package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.io.IOException;

public class DocumentBuildHandler implements IBootstrapHandler {

    private String graphQLFileName;

    public DocumentBuildHandler() {
    }

    public DocumentBuildHandler(String graphQL) {
        this.graphQLFileName = graphQL;
    }

    @Override
    public Void transform(IGraphQLDocumentManager manager, Object object) throws IOException {
        if (this.graphQLFileName != null) {
            manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream(graphQLFileName));
        }
        manager.registerDocument(new DocumentBuilder(manager).buildDocument().toString());
        return null;
    }
}