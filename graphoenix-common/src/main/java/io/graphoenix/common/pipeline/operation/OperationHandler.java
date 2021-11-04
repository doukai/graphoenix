package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
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

        switch ((OperationType) context.get(OperationConstant.OPERATION_TYPE_KEY)) {
            case QUERY:
                context.put(CURRENT_DATA_KEY, operationHandler.query(context.get(CURRENT_DATA_KEY)));
                break;
            case MUTATION:
                context.put(CURRENT_DATA_KEY, operationHandler.mutation(context.get(CURRENT_DATA_KEY)));
                break;
            case SUBSCRIPTION:
                context.put(CURRENT_DATA_KEY, operationHandler.subscription(context.get(CURRENT_DATA_KEY)));
                break;
        }
        return false;
    }
}
