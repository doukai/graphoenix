package io.graphoenix.common.pipeline;

import io.graphoenix.common.pipeline.bootstrap.BootstrapPipeline;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.graphoenix.common.pipeline.operation.OperationRouter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static io.graphoenix.common.utils.BootstrapHandlerUtil.BOOTSTRAP_HANDLER_UTIL;
import static io.graphoenix.common.utils.OperationHandlerUtil.OPERATION_HANDLER_UTIL;

public class GraphQLCodeGenerator {

    private final IGraphQLDocumentManager manager;
    private final OperationRouter router;
    private Set<IBootstrapHandler> bootstrapHandlers;
    private Set<IOperationHandler> operationHandlers;

    @Inject
    public GraphQLCodeGenerator(IGraphQLDocumentManager manager, OperationRouter router) {
        this.manager = manager;
        this.router = router;
    }

    public GraphQLCodeGenerator registerDocument(String graphQL) {
        manager.registerDocument(graphQL);
        return this;
    }

    public GraphQLCodeGenerator registerDocument(InputStream inputStream) throws IOException {
        manager.registerDocument(inputStream);
        return this;
    }

    public GraphQLCodeGenerator registerFile(String graphqlFileName) throws IOException {
        manager.registerFile(graphqlFileName);
        return this;
    }

    public GraphQLCodeGenerator registerPath(Path graphqlPath) throws IOException {
        manager.registerPath(graphqlPath);
        return this;
    }

    public GraphQLCodeGenerator addBootstrapHandlers(Set<String> bootstrapHandlerNames) throws Exception {
        if (this.bootstrapHandlers == null) {
            this.bootstrapHandlers = new HashSet<>();
        }
        for (String bootstrapHandlerName : bootstrapHandlerNames) {
            this.bootstrapHandlers.add(BOOTSTRAP_HANDLER_UTIL.get(bootstrapHandlerName));
        }
        return this;
    }

    public GraphQLCodeGenerator addOperationHandlers(Set<String> operationHandlerNames) throws Exception {
        if (this.operationHandlers == null) {
            this.operationHandlers = new HashSet<>();
        }
        for (String operationHandlerName : operationHandlerNames) {
            this.operationHandlers.add(OPERATION_HANDLER_UTIL.get(operationHandlerName));
        }
        return this;
    }

    public GraphQLCodeGenerator bootstrap() throws Exception {
        BootstrapPipeline bootstrapPipeline = new BootstrapPipeline();
        for (IBootstrapHandler bootstrapHandler : bootstrapHandlers) {
            bootstrapPipeline.addHandler(bootstrapHandler);
        }
        bootstrapPipeline.execute();
        return this;
    }

    public String generate(String graphQL) throws Exception {
        return this.createOperationPipeline().fetch(graphQL, String.class);
    }

    private OperationPipeline createOperationPipeline() {
        OperationPipeline operationPipeline = new OperationPipeline(router);
        for (IOperationHandler operationHandler : operationHandlers) {
            operationPipeline.addHandler(operationHandler);
        }
        return operationPipeline;
    }
}
