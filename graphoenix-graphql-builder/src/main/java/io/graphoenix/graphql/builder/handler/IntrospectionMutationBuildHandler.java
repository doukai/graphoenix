package io.graphoenix.graphql.builder.handler;

import com.google.auto.service.AutoService;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.bootstrap.introspection.IIntrospectionMutationBuildHandler;

@AutoService(IIntrospectionMutationBuildHandler.class)
public class IntrospectionMutationBuildHandler implements IIntrospectionMutationBuildHandler {

    @Override
    public String transform(IGraphqlDocumentManager manager, Void object) {
        Operation operation = new IntrospectionMutationBuilder(manager).buildIntrospectionSchemaMutation();
        return operation.toString();
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
