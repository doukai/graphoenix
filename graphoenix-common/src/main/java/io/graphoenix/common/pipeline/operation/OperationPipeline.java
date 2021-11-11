package io.graphoenix.common.pipeline.operation;

import com.google.gson.JsonObject;
import io.graphoenix.common.utils.HandlerUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.*;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OperationPipeline extends ChainBase {

    private IGraphQLDocumentManager manager;

    public OperationPipeline() {
        addCommand(new OperationRouter());
    }

    public OperationPipeline(IGraphQLDocumentManager manager) {
        this.manager = manager;
        addCommand(new OperationRouter());
    }

    public OperationPipeline setupManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
        return this;
    }

    public OperationPipeline addHandler(IOperationHandler handler) {
        addCommand(new OperationHandler(handler));
        return this;
    }

    public <T extends IOperationHandler> OperationPipeline addHandler(Class<T> handlerClass) {
        addCommand(new OperationHandler(HandlerUtil.HANDLER_UTIL.create(handlerClass)));
        return this;
    }

    public GraphQLResponse fetch(GraphQLRequest request) throws Exception {
        OperationContext operationContext = new OperationContext();
        operationContext.setExecuteType(ExecuteType.SYNC);
        operationContext.setAsyncType(AsyncType.OPERATION);
        operationContext.setCurrentData(request.getQuery());
        operationContext.setManager(this.manager);
        this.execute(operationContext);
        return new GraphQLResponse(operationContext.getCurrentData());
    }

    @SuppressWarnings("unchecked")
    public Mono<GraphQLResponse> fetchAsync(GraphQLRequest request) throws Exception {
        OperationContext operationContext = new OperationContext();
        operationContext.setExecuteType(ExecuteType.ASYNC);
        operationContext.setAsyncType(AsyncType.OPERATION);
        operationContext.setCurrentData(request.getQuery());
        operationContext.setManager(this.manager);
        this.execute(operationContext);
        return ((Mono<JsonObject>) operationContext.getCurrentData()).map(GraphQLResponse::new);
    }

    @SuppressWarnings("unchecked")
    public Flux<GraphQLResponse> fetchSelectionsAsync(GraphQLRequest request) throws Exception {
        OperationContext operationContext = new OperationContext();
        operationContext.setExecuteType(ExecuteType.ASYNC);
        operationContext.setAsyncType(AsyncType.SELECTION);
        operationContext.setCurrentData(request.getQuery());
        operationContext.setManager(this.manager);
        this.execute(operationContext);
        return ((Flux<JsonObject>) operationContext.getCurrentData()).map(GraphQLResponse::new);
    }
}
