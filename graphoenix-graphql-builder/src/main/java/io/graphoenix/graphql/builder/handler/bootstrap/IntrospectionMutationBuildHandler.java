package io.graphoenix.graphql.builder.handler.bootstrap;

import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;

public class IntrospectionMutationBuildHandler implements IBootstrapHandler {

    private final IGraphQLDocumentManager manager;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;

    @Inject
    public IntrospectionMutationBuildHandler(IGraphQLDocumentManager manager, IntrospectionMutationBuilder introspectionMutationBuilder) {
        this.manager = manager;
        this.introspectionMutationBuilder = introspectionMutationBuilder;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
        context.add(operation.toString());
        return false;
    }
}
