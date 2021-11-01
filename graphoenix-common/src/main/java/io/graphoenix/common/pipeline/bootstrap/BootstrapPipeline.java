package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.common.manager.*;
import io.graphoenix.spi.antlr.*;
import io.graphoenix.spi.dto.GraphQLRequestBody;
import org.apache.commons.chain.impl.ChainBase;

import java.io.IOException;
import java.io.InputStream;

public class BootstrapPipeline extends ChainBase {

    private final IGraphqlDocumentManager manager;

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

    public BootstrapPipeline() {
        this.manager = getGraphqlDocumentManager();
    }

    public BootstrapPipeline(String graphQL) {
        this();
        this.manager.registerDocument(graphQL);
    }

    public BootstrapPipeline(InputStream inputStream) throws IOException {
        this();
        this.manager.registerDocument(inputStream);
    }

    public BootstrapPipeline addHandler(BootstrapHandler handler) {
        addCommand(handler);
        return this;
    }

    public void process(GraphQLRequestBody requestBody) throws Exception {
        BootstrapContext requestContext = new BootstrapContext();
        requestContext.setManager(this.manager);
        this.execute(requestContext);
    }

    public BootstrapPipeline setup(IGraphqlOperationManager graphqlOperationManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlSchemaManager graphqlSchemaManager) {
        this.graphqlSchemaManager = graphqlSchemaManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlDirectiveManager graphqlDirectiveManager) {
        this.graphqlDirectiveManager = graphqlDirectiveManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlObjectManager graphqlObjectManager) {
        this.graphqlObjectManager = graphqlObjectManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlFieldManager graphqlFieldManager) {
        this.graphqlFieldManager = graphqlFieldManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlInputObjectManager graphqlInputObjectManager) {
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlInputValueManager graphqlInputValueManager) {
        this.graphqlInputValueManager = graphqlInputValueManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlEnumManager graphqlEnumManager) {
        this.graphqlEnumManager = graphqlEnumManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlScalarManager graphqlScalarManager) {
        this.graphqlScalarManager = graphqlScalarManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphqlFragmentManager graphqlFragmentManager) {
        this.graphqlFragmentManager = graphqlFragmentManager;
        return this;
    }

    private IGraphqlOperationManager getGraphqlOperationManager() {
        return graphqlOperationManager == null ? new GraphqlOperationManager() : graphqlOperationManager;
    }

    private IGraphqlSchemaManager getGraphqlSchemaManager() {
        return graphqlSchemaManager == null ? new GraphqlSchemaManager() : graphqlSchemaManager;
    }

    private IGraphqlDirectiveManager getGraphqlDirectiveManager() {
        return graphqlDirectiveManager == null ? new GraphqlDirectiveManager() : graphqlDirectiveManager;
    }

    private IGraphqlObjectManager getGraphqlObjectManager() {
        return graphqlObjectManager == null ? new GraphqlObjectManager() : graphqlObjectManager;
    }

    private IGraphqlFieldManager getGraphqlFieldManager() {
        return graphqlFieldManager == null ? new GraphqlFieldManager() : graphqlFieldManager;
    }

    private IGraphqlInputObjectManager getGraphqlInputObjectManager() {
        return graphqlInputObjectManager == null ? new GraphqlInputObjectManager() : graphqlInputObjectManager;
    }

    private IGraphqlInputValueManager getGraphqlInputValueManager() {
        return graphqlInputValueManager == null ? new GraphqlInputValueManager() : graphqlInputValueManager;
    }

    private IGraphqlEnumManager getGraphqlEnumManager() {
        return graphqlEnumManager == null ? new GraphqlEnumManager() : graphqlEnumManager;
    }

    private IGraphqlScalarManager getGraphqlScalarManager() {
        return graphqlScalarManager == null ? new GraphqlScalarManager() : graphqlScalarManager;
    }

    private IGraphqlFragmentManager getGraphqlFragmentManager() {
        return graphqlFragmentManager == null ? new GraphqlFragmentManager() : graphqlFragmentManager;
    }

    private IGraphqlDocumentManager getGraphqlDocumentManager() {
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
}
