package io.graphoenix.common.pipeline;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import io.graphoenix.common.pipeline.bootstrap.BootstrapPipeline;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.GraphQLResponse;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AutoFactory
public class GraphQLDataFetcher {

    private final IGraphqlDocumentManager documentManager;
    private final IOperationHandler[] operationHandlers;

    public GraphQLDataFetcher(@Provided IBootstrapHandler[] bootstrapHandlers,
                              @Provided IOperationHandler[] operationHandlers) throws Exception {
        BootstrapPipeline bootstrapPipeline = new BootstrapPipeline();
        for (IBootstrapHandler bootstrapHandler : bootstrapHandlers) {
            bootstrapPipeline.addHandler(bootstrapHandler);
        }
        this.documentManager = bootstrapPipeline.buildManager();
        this.operationHandlers = operationHandlers;
    }

    public GraphQLResponse fetch(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetch(request);
    }

    public Mono<GraphQLResponse> fetchAsync(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetchAsync(request);
    }

    public Flux<GraphQLResponse> fetchSelectionsAsync(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetchSelectionsAsync(request);
    }

    private OperationPipeline createOperationPipeline() {
        OperationPipeline operationPipeline = new OperationPipeline(this.documentManager);
        for (IOperationHandler operationHandler : operationHandlers) {
            operationPipeline.addHandler(operationHandler);
        }
        return operationPipeline;
    }
}
