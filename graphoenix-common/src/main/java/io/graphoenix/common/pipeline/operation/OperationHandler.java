package io.graphoenix.common.pipeline.operation;

import io.graphoenix.spi.handler.OperationType;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.operation.OperationConstant.CURRENT_DATA_KEY;
import static io.graphoenix.common.pipeline.operation.OperationConstant.OPERATION_TYPE_KEY;

public abstract class OperationHandler implements Command {

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {

        switch ((OperationType) context.get(OPERATION_TYPE_KEY)) {
            case QUERY:
                context.put(CURRENT_DATA_KEY, query(context.get(CURRENT_DATA_KEY)));
                break;
            case MUTATION:
                context.put(CURRENT_DATA_KEY, mutation(context.get(CURRENT_DATA_KEY)));
                break;
            case SUBSCRIPTION:
                context.put(CURRENT_DATA_KEY, subscription(context.get(CURRENT_DATA_KEY)));
                break;
        }
        return false;
    }

    abstract Object query(Object input) throws Exception;

    abstract Object mutation(Object input) throws Exception;

    abstract Object subscription(Object input) throws Exception;
}
