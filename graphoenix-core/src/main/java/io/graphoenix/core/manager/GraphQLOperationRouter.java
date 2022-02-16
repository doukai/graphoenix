package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.graphoenix.spi.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;

@ApplicationScoped
public class GraphQLOperationRouter {

    private final IGraphQLDocumentManager manager;

    @Inject
    public GraphQLOperationRouter(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public OperationType getType(String graphql) {

        GraphqlParser.OperationTypeContext operationTypeContext = manager.getOperationType(graphql);
        if (operationTypeContext == null || operationTypeContext.QUERY() != null) {
            return OperationType.QUERY;
        } else if (operationTypeContext.MUTATION() != null) {
            return OperationType.MUTATION;
        } else if (operationTypeContext.SUBSCRIPTION() != null) {
            return OperationType.SUBSCRIPTION;
        }
        throw new GraphQLProblem(UNSUPPORTED_OPERATION_TYPE);
    }
}
