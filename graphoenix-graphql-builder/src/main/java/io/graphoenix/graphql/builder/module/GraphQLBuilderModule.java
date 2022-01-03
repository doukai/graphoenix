package io.graphoenix.graphql.builder.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.core.module.DocumentManagerModule;
import io.graphoenix.graphql.builder.config.GraphQLBuilderConfig;
import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.graphql.builder.handler.bootstrap.IntrospectionMutationBuildHandler;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class GraphQLBuilderModule {

    @ConfigProperty
    private GraphQLBuilderConfig graphQLBuilderConfig;

    @Provides
    @Singleton
    public DocumentBuilder documentBuilder(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, GraphQLConfigRegister graphQLConfigRegister) {
        return new DocumentBuilder(graphQLBuilderConfig, manager, mapper, graphQLConfigRegister);
    }

    @Provides
    @Singleton
    public IntrospectionMutationBuilder introspectionMutationBuilder(IGraphQLDocumentManager manager) {
        return new IntrospectionMutationBuilder(manager);
    }

    @Provides
    @Singleton
    public DocumentBuildHandler documentBuildHandler(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, GraphQLConfigRegister graphQLConfigRegister) {
        return new DocumentBuildHandler(documentBuilder(manager, mapper, graphQLConfigRegister));
    }

    @Provides
    @Singleton
    public IntrospectionMutationBuildHandler introspectionMutationBuildHandler(IGraphQLDocumentManager manager) {
        return new IntrospectionMutationBuildHandler(manager, introspectionMutationBuilder(manager));
    }
}
