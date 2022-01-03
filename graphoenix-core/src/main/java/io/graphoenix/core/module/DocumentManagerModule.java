package io.graphoenix.core.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.manager.*;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.spi.antlr.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module
public class DocumentManagerModule {

    @ConfigProperty
    GraphQLConfig graphQLConfig;

    @Provides
    @Singleton
    IGraphQLOperationManager graphQLOperationManager() {
        return new GraphQLOperationManager();
    }

    @Provides
    @Singleton
    IGraphQLSchemaManager graphQLSchemaManager() {
        return new GraphQLSchemaManager();
    }

    @Provides
    @Singleton
    IGraphQLDirectiveManager graphQLDirectiveManager() {
        return new GraphQLDirectiveManager();
    }

    @Provides
    @Singleton
    IGraphQLObjectManager graphQLObjectManager() {
        return new GraphQLObjectManager();
    }

    @Provides
    @Singleton
    IGraphQLInterfaceManager graphQLInterfaceManager() {
        return new GraphQLInterfaceManager();
    }

    @Provides
    @Singleton
    IGraphQLUnionManager graphQLUnionManager() {
        return new GraphQLUnionManager();
    }

    @Provides
    @Singleton
    IGraphQLFieldManager graphQLFieldManager() {
        return new GraphQLFieldManager();
    }

    @Provides
    @Singleton
    IGraphQLInputObjectManager graphQLInputObjectManager() {
        return new GraphQLInputObjectManager();
    }

    @Provides
    @Singleton
    IGraphQLInputValueManager graphQLInputValueManager() {
        return new GraphQLInputValueManager();
    }

    @Provides
    @Singleton
    IGraphQLEnumManager graphQLEnumManager() {
        return new GraphQLEnumManager();
    }

    @Provides
    @Singleton
    IGraphQLScalarManager graphQLScalarManager() {
        return new GraphQLScalarManager();
    }

    @Provides
    @Singleton
    IGraphQLFragmentManager graphQLFragmentManager() {
        return new GraphQLFragmentManager();
    }

    @Provides
    @Singleton
    IGraphQLDocumentManager graphQLDocumentManager() {
        return new GraphQLDocumentManager(
                graphQLOperationManager(),
                graphQLSchemaManager(),
                graphQLDirectiveManager(),
                graphQLObjectManager(),
                graphQLInterfaceManager(),
                graphQLUnionManager(),
                graphQLFieldManager(),
                graphQLInputObjectManager(),
                graphQLInputValueManager(),
                graphQLEnumManager(),
                graphQLScalarManager(),
                graphQLFragmentManager()
        );
    }

    @Provides
    @Singleton
    IGraphQLFieldMapManager graphQLFieldMapManager(IGraphQLDocumentManager manager) {
        return new GraphQLFieldMapManager(manager);
    }

    @Provides
    @Singleton
    GraphQLOperationRouter graphQLOperationRouter(IGraphQLDocumentManager manager) {
        return new GraphQLOperationRouter(manager);
    }

    @Provides
    @Singleton
    GraphQLConfigRegister graphQLRegister(IGraphQLDocumentManager manager) {
        return new GraphQLConfigRegister(graphQLConfig, manager);
    }
}
