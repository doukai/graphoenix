package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;

@ApplicationScoped
public class GraphQLOperationRouter {

    public OperationType getType(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {

        GraphqlParser.OperationTypeContext operationTypeContext = operationDefinitionContext.operationType();
        if (operationTypeContext == null || operationTypeContext.QUERY() != null) {
            return OperationType.QUERY;
        } else if (operationTypeContext.MUTATION() != null) {
            return OperationType.MUTATION;
        } else if (operationTypeContext.SUBSCRIPTION() != null) {
            return OperationType.SUBSCRIPTION;
        }
        throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
    }
}
