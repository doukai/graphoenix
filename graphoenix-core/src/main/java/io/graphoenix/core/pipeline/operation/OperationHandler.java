package io.graphoenix.core.pipeline.operation;

import io.graphoenix.core.pipeline.PipelineContext;
import io.graphoenix.spi.dto.type.AsyncType;
import io.graphoenix.spi.dto.type.ExecuteType;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.core.pipeline.PipelineContext.INSTANCE_KEY;

public class OperationHandler implements Command {

    private final IOperationHandler operationHandler;

    public OperationHandler(IOperationHandler operationHandler) {
        this.operationHandler = operationHandler;
    }

    @Override
    public boolean execute(Context context) throws Exception {

        PipelineContext pipelineContext = (PipelineContext) context.get(INSTANCE_KEY);
        OperationType operationType = pipelineContext.getStatus(OperationType.class);
        ExecuteType executeType = pipelineContext.getStatus(ExecuteType.class);
        AsyncType asyncType = pipelineContext.getStatus(AsyncType.class);

        switch (operationType) {
            case QUERY:
                switch (executeType) {
                    case SYNC:
                        return operationHandler.query(pipelineContext);
                    case ASYNC:
                        switch (asyncType) {
                            case OPERATION:
                                return operationHandler.queryAsync(pipelineContext);
                            case SELECTION:
                                return operationHandler.querySelectionsAsync(pipelineContext);
                        }
                }
            case MUTATION:
                switch (executeType) {
                    case SYNC:
                        return operationHandler.mutation(pipelineContext);
                    case ASYNC:
                        return operationHandler.mutationAsync(pipelineContext);
                }
            case SUBSCRIPTION:
                return operationHandler.subscription(pipelineContext);
            default:
                return true;
        }
    }
}
