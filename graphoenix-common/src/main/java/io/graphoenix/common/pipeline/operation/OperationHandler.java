package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.OperationType;
import io.graphoenix.spi.handler.operation.IOperationHandler;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.operation.OperationConstant.MANAGER_KEY;
import static io.graphoenix.common.pipeline.operation.OperationConstant.CURRENT_DATA_KEY;

public class OperationHandler<I, O> implements Command {

    private final IOperationHandler<I, O> operationHandler;

    public OperationHandler(IOperationHandler<I, O> operationHandler) {
        this.operationHandler = operationHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {

        operationHandler.setupManager((IGraphqlDocumentManager) context.get(MANAGER_KEY));

        switch ((OperationType) context.get(OperationConstant.OPERATION_TYPE_KEY)) {
            case QUERY:
                context.put(CURRENT_DATA_KEY, operationHandler.query((I) context.get(CURRENT_DATA_KEY)));
                break;
            case MUTATION:
                context.put(CURRENT_DATA_KEY, operationHandler.mutation((I) context.get(CURRENT_DATA_KEY)));
                break;
            case SUBSCRIPTION:
                context.put(CURRENT_DATA_KEY, operationHandler.subscription((I) context.get(CURRENT_DATA_KEY)));
                break;
        }
        return false;
    }
}
