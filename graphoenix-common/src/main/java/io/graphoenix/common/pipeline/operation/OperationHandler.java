package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.AsyncType;
import io.graphoenix.spi.dto.ExecuteType;
import io.graphoenix.spi.dto.OperationType;
import io.graphoenix.spi.handler.IOperationHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.operation.OperationConstant.MANAGER_KEY;
import static io.graphoenix.common.pipeline.operation.OperationConstant.CURRENT_DATA_KEY;

public class OperationHandler implements Command {

    private final IOperationHandler operationHandler;

    public OperationHandler(IOperationHandler operationHandler) {
        this.operationHandler = operationHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {

        operationHandler.setupManager((IGraphqlDocumentManager) context.get(MANAGER_KEY));
        OperationType operationType = (OperationType) context.get(OperationConstant.OPERATION_TYPE_KEY);
        ExecuteType executeType = (ExecuteType) context.get(OperationConstant.EXECUTE_TYPE_KEY);
        AsyncType asyncType = (AsyncType) context.get(OperationConstant.ASYNC_TYPE_KEY);

        switch (operationType) {
            case QUERY:
                switch (executeType) {
                    case SYNC:
                        context.put(CURRENT_DATA_KEY, operationHandler.query(context.get(CURRENT_DATA_KEY)));
                        break;
                    case ASYNC:
                        switch (asyncType) {
                            case OPERATION:
                                context.put(CURRENT_DATA_KEY, operationHandler.queryAsync(context.get(CURRENT_DATA_KEY)));
                                break;
                            case SELECTION:
                                context.put(CURRENT_DATA_KEY, operationHandler.querySelectionsAsync(context.get(CURRENT_DATA_KEY)));
                                break;
                        }
                }
            case MUTATION:
                switch (executeType) {
                    case SYNC:
                        context.put(CURRENT_DATA_KEY, operationHandler.mutation(context.get(CURRENT_DATA_KEY)));
                        break;
                    case ASYNC:
                        context.put(CURRENT_DATA_KEY, operationHandler.mutationAsync(context.get(CURRENT_DATA_KEY)));
                        break;
                }
            case SUBSCRIPTION:
                context.put(CURRENT_DATA_KEY, operationHandler.subscription(context.get(CURRENT_DATA_KEY)));
                break;
        }
        return false;
    }
}
