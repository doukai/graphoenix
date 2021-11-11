package io.graphoenix.common.pipeline.bootstrap;

import io.graphoenix.common.utils.HandlerUtil;
import io.graphoenix.common.manager.*;
import io.graphoenix.spi.antlr.*;
import io.graphoenix.spi.handler.IBootstrapHandler;
import org.apache.commons.chain.impl.ChainBase;

import java.io.IOException;
import java.io.InputStream;

public class BootstrapPipeline extends ChainBase {

    private final IGraphQLDocumentManager manager;

    private IGraphQLOperationManager graphQLOperationManager;

    private IGraphQLSchemaManager graphQLSchemaManager;

    private IGraphQLDirectiveManager graphQLDirectiveManager;

    private IGraphQLObjectManager graphQLObjectManager;

    private IGraphQLInterfaceManager graphQLInterfaceManager;

    private IGraphQLUnionManager graphQLUnionManager;

    private IGraphQLFieldManager graphQLFieldManager;

    private IGraphQLInputObjectManager graphQLInputObjectManager;

    private IGraphQLInputValueManager graphQLInputValueManager;

    private IGraphQLEnumManager graphQLEnumManager;

    private IGraphQLScalarManager graphQLScalarManager;

    private IGraphQLFragmentManager graphQLFragmentManager;

    public BootstrapPipeline() {
        this.manager = getGraphQLDocumentManager();
    }

    public BootstrapPipeline(String graphQL) {
        this();
        this.manager.registerDocument(graphQL);
    }

    public BootstrapPipeline(InputStream inputStream) throws IOException {
        this();
        this.manager.registerDocument(inputStream);
    }

    public IGraphQLDocumentManager buildManager() throws Exception {
        BootstrapContext bootstrapContext = new BootstrapContext();
        bootstrapContext.setManager(this.manager);
        this.execute(bootstrapContext);
        return manager;
    }

    public BootstrapPipeline addHandler(IBootstrapHandler handler) {
        addCommand(new BootstrapHandler(handler));
        return this;
    }

    public <T extends IBootstrapHandler> BootstrapPipeline addHandler(Class<T> handlerClass) {
        addCommand(new BootstrapHandler(HandlerUtil.HANDLER_UTIL.create(handlerClass)));
        return this;
    }

    public BootstrapPipeline setup(IGraphQLOperationManager graphQLOperationManager) {
        this.graphQLOperationManager = graphQLOperationManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLSchemaManager graphQLSchemaManager) {
        this.graphQLSchemaManager = graphQLSchemaManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLDirectiveManager graphQLDirectiveManager) {
        this.graphQLDirectiveManager = graphQLDirectiveManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLObjectManager graphQLObjectManager) {
        this.graphQLObjectManager = graphQLObjectManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLInterfaceManager graphQLInterfaceManager) {
        this.graphQLInterfaceManager = graphQLInterfaceManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLUnionManager graphQLUnionManager) {
        this.graphQLUnionManager = graphQLUnionManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLFieldManager graphQLFieldManager) {
        this.graphQLFieldManager = graphQLFieldManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLInputObjectManager graphQLInputObjectManager) {
        this.graphQLInputObjectManager = graphQLInputObjectManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLInputValueManager graphQLInputValueManager) {
        this.graphQLInputValueManager = graphQLInputValueManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLEnumManager graphQLEnumManager) {
        this.graphQLEnumManager = graphQLEnumManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLScalarManager graphQLScalarManager) {
        this.graphQLScalarManager = graphQLScalarManager;
        return this;
    }

    public BootstrapPipeline setup(IGraphQLFragmentManager graphQLFragmentManager) {
        this.graphQLFragmentManager = graphQLFragmentManager;
        return this;
    }

    private IGraphQLOperationManager getGraphQLOperationManager() {
        return graphQLOperationManager == null ? new GraphQLOperationManager() : graphQLOperationManager;
    }

    private IGraphQLSchemaManager getGraphQLSchemaManager() {
        return graphQLSchemaManager == null ? new GraphQLSchemaManager() : graphQLSchemaManager;
    }

    private IGraphQLDirectiveManager getGraphQLDirectiveManager() {
        return graphQLDirectiveManager == null ? new GraphQLDirectiveManager() : graphQLDirectiveManager;
    }

    private IGraphQLObjectManager getGraphQLObjectManager() {
        return graphQLObjectManager == null ? new GraphQLObjectManager() : graphQLObjectManager;
    }

    private IGraphQLInterfaceManager getGraphQLInterfaceManager() {
        return graphQLInterfaceManager == null ? new GraphQLInterfaceManager() : graphQLInterfaceManager;
    }

    private IGraphQLUnionManager getGraphQLUnionManager() {
        return graphQLUnionManager == null ? new GraphQLUnionManager() : graphQLUnionManager;
    }

    private IGraphQLFieldManager getGraphQLFieldManager() {
        return graphQLFieldManager == null ? new GraphQLFieldManager() : graphQLFieldManager;
    }

    private IGraphQLInputObjectManager getGraphQLInputObjectManager() {
        return graphQLInputObjectManager == null ? new GraphQLInputObjectManager() : graphQLInputObjectManager;
    }

    private IGraphQLInputValueManager getGraphQLInputValueManager() {
        return graphQLInputValueManager == null ? new GraphQLInputValueManager() : graphQLInputValueManager;
    }

    private IGraphQLEnumManager getGraphQLEnumManager() {
        return graphQLEnumManager == null ? new GraphQLEnumManager() : graphQLEnumManager;
    }

    private IGraphQLScalarManager getGraphQLScalarManager() {
        return graphQLScalarManager == null ? new GraphQLScalarManager() : graphQLScalarManager;
    }

    private IGraphQLFragmentManager getGraphQLFragmentManager() {
        return graphQLFragmentManager == null ? new GraphQLFragmentManager() : graphQLFragmentManager;
    }

    private IGraphQLDocumentManager getGraphQLDocumentManager() {
        return new GraphqlDocumentManagerFactory(
                this::getGraphQLOperationManager,
                this::getGraphQLSchemaManager,
                this::getGraphQLDirectiveManager,
                this::getGraphQLObjectManager,
                this::getGraphQLInterfaceManager,
                this::getGraphQLUnionManager,
                this::getGraphQLFieldManager,
                this::getGraphQLInputObjectManager,
                this::getGraphQLInputValueManager,
                this::getGraphQLEnumManager,
                this::getGraphQLScalarManager,
                this::getGraphQLFragmentManager
        ).create();
    }
}
