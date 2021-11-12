package io.graphoenix.common.pipeline.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.operation.OperationConstant.*;

public class OperationRouter implements Command {

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) {
        String graphQL = (String) context.get(CURRENT_DATA_KEY);
        IGraphQLDocumentManager manager = (IGraphQLDocumentManager) context.get(MANAGER_KEY);
        GraphqlParser.OperationTypeContext operationTypeContext = manager.getOperationType(graphQL);
        if (operationTypeContext == null || operationTypeContext.QUERY() != null) {
            context.put(OPERATION_TYPE_KEY, OperationType.QUERY);
        } else if (operationTypeContext.MUTATION() != null) {
            context.put(OPERATION_TYPE_KEY, OperationType.MUTATION);
        } else if (operationTypeContext.SUBSCRIPTION() != null) {
            context.put(OPERATION_TYPE_KEY, OperationType.SUBSCRIPTION);
        }
        return false;
    }
}
