package io.graphoenix.common.handler;

import io.graphoenix.common.manager.*;
import io.graphoenix.spi.antlr.*;

public enum GraphQLOperationPipelineBootstrap {

    GRAPHQL_OPERATION_PIPELINE_BOOTSTRAP;

    private IGraphqlOperationManager graphqlOperationManager;

    private IGraphqlSchemaManager graphqlSchemaManager;

    private IGraphqlDirectiveManager graphqlDirectiveManager;

    private IGraphqlObjectManager graphqlObjectManager;

    private IGraphqlFieldManager graphqlFieldManager;

    private IGraphqlInputObjectManager graphqlInputObjectManager;

    private IGraphqlInputValueManager graphqlInputValueManager;

    private IGraphqlEnumManager graphqlEnumManager;

    private IGraphqlScalarManager graphqlScalarManager;

    private IGraphqlFragmentManager graphqlFragmentManager;

    public GraphQLOperationPipelineBootstrap setup(IGraphqlOperationManager graphqlOperationManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlSchemaManager graphqlSchemaManager) {
        this.graphqlSchemaManager = graphqlSchemaManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlDirectiveManager graphqlDirectiveManager) {
        this.graphqlDirectiveManager = graphqlDirectiveManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlObjectManager graphqlObjectManager) {
        this.graphqlObjectManager = graphqlObjectManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlFieldManager graphqlFieldManager) {
        this.graphqlFieldManager = graphqlFieldManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlInputObjectManager graphqlInputObjectManager) {
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlInputValueManager graphqlInputValueManager) {
        this.graphqlInputValueManager = graphqlInputValueManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlEnumManager graphqlEnumManager) {
        this.graphqlEnumManager = graphqlEnumManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlScalarManager graphqlScalarManager) {
        this.graphqlScalarManager = graphqlScalarManager;
        return this;
    }

    public GraphQLOperationPipelineBootstrap setup(IGraphqlFragmentManager graphqlFragmentManager) {
        this.graphqlFragmentManager = graphqlFragmentManager;
        return this;
    }

    public IGraphqlOperationManager getGraphqlOperationManager() {
        return graphqlOperationManager == null ? new GraphqlOperationManager() : graphqlOperationManager;
    }

    public IGraphqlSchemaManager getGraphqlSchemaManager() {
        return graphqlSchemaManager == null ? new GraphqlSchemaManager() : graphqlSchemaManager;
    }

    public IGraphqlDirectiveManager getGraphqlDirectiveManager() {
        return graphqlDirectiveManager == null ? new GraphqlDirectiveManager() : graphqlDirectiveManager;
    }

    public IGraphqlObjectManager getGraphqlObjectManager() {
        return graphqlObjectManager == null ? new GraphqlObjectManager() : graphqlObjectManager;
    }

    public IGraphqlFieldManager getGraphqlFieldManager() {
        return graphqlFieldManager == null ? new GraphqlFieldManager() : graphqlFieldManager;
    }

    public IGraphqlInputObjectManager getGraphqlInputObjectManager() {
        return graphqlInputObjectManager == null ? new GraphqlInputObjectManager() : graphqlInputObjectManager;
    }

    public IGraphqlInputValueManager getGraphqlInputValueManager() {
        return graphqlInputValueManager == null ? new GraphqlInputValueManager() : graphqlInputValueManager;
    }

    public IGraphqlEnumManager getGraphqlEnumManager() {
        return graphqlEnumManager == null ? new GraphqlEnumManager() : graphqlEnumManager;
    }

    public IGraphqlScalarManager getGraphqlScalarManager() {
        return graphqlScalarManager == null ? new GraphqlScalarManager() : graphqlScalarManager;
    }

    public IGraphqlFragmentManager getGraphqlFragmentManager() {
        return graphqlFragmentManager == null ? new GraphqlFragmentManager() : graphqlFragmentManager;
    }

    public IGraphqlDocumentManager getGraphqlDocumentManager() {
        return new GraphqlDocumentManagerFactory(
                this::getGraphqlOperationManager,
                this::getGraphqlSchemaManager,
                this::getGraphqlDirectiveManager,
                this::getGraphqlObjectManager,
                this::getGraphqlFieldManager,
                this::getGraphqlInputObjectManager,
                this::getGraphqlInputValueManager,
                this::getGraphqlEnumManager,
                this::getGraphqlScalarManager,
                this::getGraphqlFragmentManager
        ).create();
    }

    public GraphQLOperationPipeline startup() {
        return new GraphQLOperationPipelineFactory(this::getGraphqlDocumentManager).create();
    }
}
