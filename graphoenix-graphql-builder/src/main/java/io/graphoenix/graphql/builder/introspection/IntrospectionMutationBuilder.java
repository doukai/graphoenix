package io.graphoenix.graphql.builder.introspection;

import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;

import java.io.IOException;
import java.io.StringWriter;

public class IntrospectionMutationBuilder {

    private final GraphqlAntlrManager manager;

    public IntrospectionMutationBuilder(GraphqlAntlrManager manager) {
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
