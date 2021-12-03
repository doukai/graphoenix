package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

public class IntrospectionMutationBuildHandler implements IBootstrapHandler {

    @Override
    public boolean execute(IPipelineContext context) {
        IGraphQLDocumentManager manager = context.getManager();
        Operation operation = new IntrospectionMutationBuilder(manager).buildIntrospectionSchemaMutation();
        context.add(operation.toString());
        return false;
    }
}
