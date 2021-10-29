package io.graphoenix.graphql.builder.schema;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

import java.io.IOException;
import java.io.StringWriter;

public class GraphqlSchemaRegister {

    private final IGraphqlDocumentManager manager;

    public GraphqlSchemaRegister(IGraphqlDocumentManager manager) {
        this.manager = manager;
    }

    public void register() throws IOException {
        StringWriter stringWriter = new StringWriter();
        GraphqlSchemaBuilder graphqlSchemaBuilder = new GraphqlSchemaBuilder(manager);
        graphqlSchemaBuilder.buildObjectExpressions(stringWriter);
        manager.registerDocument(stringWriter.toString());
        stringWriter.close();
    }
}
