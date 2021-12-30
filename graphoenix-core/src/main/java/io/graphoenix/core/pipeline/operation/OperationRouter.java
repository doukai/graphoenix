package io.graphoenix.core.pipeline.operation;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;

import javax.inject.Inject;

public class OperationRouter {

    private final IGraphQLDocumentManager manager;

    @Inject
    public OperationRouter(IGraphQLDocumentManager manager) {
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
        return null;
    }
}
