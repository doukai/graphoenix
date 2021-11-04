package io.graphoenix.common.pipeline.operation;

import io.graphoenix.common.utils.HandlerUtil;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.GraphQLResponse;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.impl.ChainBase;

public class OperationPipeline extends ChainBase {

    private IGraphqlDocumentManager manager;

    public OperationPipeline() {
        addCommand(new OperationRouter());
    }

    public OperationPipeline(IGraphqlDocumentManager manager) {
        this.manager = manager;
        addCommand(new OperationRouter());
    }

    public OperationPipeline setupManager(IGraphqlDocumentManager manager) {
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

    public GraphQLResponse process(GraphQLRequest requestBody) throws Exception {
        OperationContext operationContext = new OperationContext();
        operationContext.setManager(this.manager);
        this.execute(operationContext);
        return (GraphQLResponse) operationContext.getCurrentData();
    }
}
