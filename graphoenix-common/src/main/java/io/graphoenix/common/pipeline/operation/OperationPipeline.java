package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.GraphQLResult;
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

    public OperationPipeline addHandler(OperationHandler handler) {
        addCommand(handler);
        return this;
    }

    public GraphQLResult process(GraphQLRequestBody requestBody) throws Exception {
        OperationContext operationContext = new OperationContext();
        operationContext.setManager(this.manager);
        this.execute(operationContext);
        return (GraphQLResult) operationContext.getCurrentData();
    }
}
