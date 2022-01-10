package io.graphoenix.core.module;

import io.graphoenix.core.manager.GraphQLVariablesProcessor;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.core.manager.GraphQLDirectiveManager;
import io.graphoenix.core.manager.GraphQLDocumentManager;
import io.graphoenix.core.manager.GraphQLEnumManager;
import io.graphoenix.core.manager.GraphQLFieldManager;
import io.graphoenix.core.manager.GraphQLFieldMapManager;
import io.graphoenix.core.manager.GraphQLFragmentManager;
import io.graphoenix.core.manager.GraphQLInputObjectManager;
import io.graphoenix.core.manager.GraphQLInputValueManager;
import io.graphoenix.core.manager.GraphQLInterfaceManager;
import io.graphoenix.core.manager.GraphQLObjectManager;
import io.graphoenix.core.manager.GraphQLOperationManager;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.core.manager.GraphQLScalarManager;
import io.graphoenix.core.manager.GraphQLSchemaManager;
import io.graphoenix.core.manager.GraphQLUnionManager;
import io.graphoenix.spi.antlr.IGraphQLDirectiveManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLEnumManager;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLFragmentManager;
import io.graphoenix.spi.antlr.IGraphQLInputObjectManager;
import io.graphoenix.spi.antlr.IGraphQLInputValueManager;
import io.graphoenix.spi.antlr.IGraphQLInterfaceManager;
import io.graphoenix.spi.antlr.IGraphQLObjectManager;
import io.graphoenix.spi.antlr.IGraphQLOperationManager;
import io.graphoenix.spi.antlr.IGraphQLScalarManager;
import io.graphoenix.spi.antlr.IGraphQLSchemaManager;
import io.graphoenix.spi.antlr.IGraphQLUnionManager;
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
    IGraphQLFieldMapManager graphQLFieldMapManager() {
        return new GraphQLFieldMapManager(graphQLDocumentManager());
    }

    @Provides
    @Singleton
    GraphQLOperationRouter graphQLOperationRouter() {
        return new GraphQLOperationRouter(graphQLDocumentManager());
    }

    @Provides
    @Singleton
    GraphQLConfigRegister graphQLRegister() {
        return new GraphQLConfigRegister(graphQLConfig, graphQLDocumentManager());
    }

    @Provides
    @Singleton
    GraphQLVariablesProcessor graphQLVariablesProcessor() {
        return new GraphQLVariablesProcessor();
    }
}
