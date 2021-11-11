package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

public class IntrospectionMutationBuildHandler implements IBootstrapHandler {

    @Override
    public String transform(IGraphQLDocumentManager manager, Object object) {
        Operation operation = new IntrospectionMutationBuilder(manager).buildIntrospectionSchemaMutation();
        return operation.toString();
    }
}
