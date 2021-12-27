package io.graphoenix.common.pipeline;

import io.graphoenix.common.pipeline.bootstrap.BootstrapPipeline;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.graphoenix.common.pipeline.operation.OperationRouter;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class GraphQLDataFetcher2 {

    private final OperationRouter router;

    private Set<IBootstrapHandler> bootstrapHandlers;
    private Set<IOperationHandler> operationHandlers;

    @Inject
    public GraphQLDataFetcher2(OperationRouter router) {
        this.router = router;
    }

    public GraphQLDataFetcher2 addBootstrapHandler(IBootstrapHandler bootstrapHandler) {
        if (bootstrapHandler == null) {
            bootstrapHandlers = new HashSet<>();
        }
        this.bootstrapHandlers.add(bootstrapHandler);
        return this;
    }

    public GraphQLDataFetcher2 bootstrap() throws Exception {
        BootstrapPipeline bootstrapPipeline = new BootstrapPipeline();
        for (IBootstrapHandler bootstrapHandler : bootstrapHandlers) {
            bootstrapPipeline.addHandler(bootstrapHandler);
        }
        bootstrapPipeline.execute();
        return this;
    }

    public GraphQLDataFetcher2 addOperationHandler(IOperationHandler operationHandler) {
        if (operationHandlers == null) {
            operationHandlers = new HashSet<>();
        }
        this.operationHandlers.add(operationHandler);
        return this;
    }

    public String fetch(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetch(request.getQuery(), String.class);
    }

    public Mono<String> fetchAsync(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetchAsyncToMono(request.getQuery(), String.class);
    }

    public Flux<Pair<String, String>> fetchSelectionsAsync(GraphQLRequest request) throws Exception {
        return this.createOperationPipeline().fetchSelectionsAsyncToFlux(request.getQuery(), String.class, String.class);
    }

    private OperationPipeline createOperationPipeline() {
        OperationPipeline operationPipeline = new OperationPipeline(router);
        for (IOperationHandler operationHandler : operationHandlers) {
            operationPipeline.addHandler(operationHandler);
        }
        return operationPipeline;
    }
}
