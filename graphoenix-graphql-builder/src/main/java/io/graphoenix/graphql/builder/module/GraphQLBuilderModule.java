package io.graphoenix.graphql.builder.module;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class GraphQLBuilderModule {

    @ConfigProperty
    private GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;
    private final GraphQLConfigRegister graphQLConfigRegister;

    @Inject
    public GraphQLBuilderModule(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, GraphQLConfigRegister graphQLConfigRegister) {
        this.manager = manager;
        this.mapper = mapper;
        this.graphQLConfigRegister = graphQLConfigRegister;
    }

    @Provides
    @Singleton
    public DocumentBuilder documentBuilder() {
        return new DocumentBuilder(graphQLConfig, manager, mapper, graphQLConfigRegister);
    }

    @Provides
    @Singleton
    public IntrospectionMutationBuilder introspectionMutationBuilder() {
        return new IntrospectionMutationBuilder(manager);
    }
}
