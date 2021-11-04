package io.graphoenix.common.pipeline.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.OperationType;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import static io.graphoenix.common.pipeline.operation.OperationConstant.*;

public class OperationRouter implements Command {

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) {
        GraphQLRequest request = (GraphQLRequest) context.get(CURRENT_DATA_KEY);
        IGraphqlDocumentManager manager = (IGraphqlDocumentManager) context.get(MANAGER_KEY);
        GraphqlParser.OperationTypeContext operationTypeContext = manager.getOperationType(request.getQuery());
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
