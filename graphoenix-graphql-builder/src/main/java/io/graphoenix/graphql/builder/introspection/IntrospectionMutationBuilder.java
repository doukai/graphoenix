package io.graphoenix.graphql.builder.introspection;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

import java.io.IOException;
import java.io.StringWriter;

public class IntrospectionMutationBuilder {

    private final IGraphqlDocumentManager manager;

    public IntrospectionMutationBuilder(IGraphqlDocumentManager manager) {
        this.manager = manager;
    }

    public String build() throws IOException {
        StringWriter stringWriter = new StringWriter();
        IntrospectionBuilder introspectionBuilder = new IntrospectionBuilder(manager);
        introspectionBuilder.buildObjectExpressions(stringWriter);
        String mutation = stringWriter.toString();
        stringWriter.close();
        return mutation;
    }
}
