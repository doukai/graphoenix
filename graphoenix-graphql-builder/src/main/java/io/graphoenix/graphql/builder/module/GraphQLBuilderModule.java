package io.graphoenix.graphql.builder.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.DocumentManagerModule;
import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.graphql.builder.handler.bootstrap.IntrospectionMutationBuildHandler;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class GraphQLBuilderModule {

    @Provides
    @Singleton
    public DocumentBuilder documentBuilder(IGraphQLDocumentManager manager) {
        return new DocumentBuilder(manager);
    }

    @Provides
    @Singleton
    public IntrospectionMutationBuilder introspectionMutationBuilder(IGraphQLDocumentManager manager) {
        return new IntrospectionMutationBuilder(manager);
    }

    @Provides
    @Singleton
    public DocumentBuildHandler documentBuildHandler(IGraphQLDocumentManager manager) {
        return new DocumentBuildHandler(documentBuilder(manager));
    }

    @Provides
    @Singleton
    public IntrospectionMutationBuildHandler introspectionMutationBuildHandler(IGraphQLDocumentManager manager) {
        return new IntrospectionMutationBuildHandler(manager, introspectionMutationBuilder(manager));
    }
}
