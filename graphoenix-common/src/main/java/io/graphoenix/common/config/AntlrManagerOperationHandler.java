package io.graphoenix.common.config;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.meta.OperationType;
import io.graphoenix.meta.dto.GraphQLRequestBody;
import io.graphoenix.meta.spi.IGraphQLOperationHandler;

public abstract class AntlrManagerOperationHandler implements IGraphQLOperationHandler {

    private final GraphqlAntlrManager graphqlAntlrManager;

    public AntlrManagerOperationHandler() {
        this.graphqlAntlrManager = new GraphqlAntlrManager();
    }

    @Override
    public OperationType getType(GraphQLRequestBody requestBody) {
        GraphqlParser.OperationTypeContext operationType = this.graphqlAntlrManager.getOperationType(requestBody.getOperationName());
        if (operationType.QUERY() != null) {
            return OperationType.QUERY;
        } else if (operationType.MUTATION() != null) {
            return OperationType.MUTATION;
        } else if (operationType.SUBSCRIPTION() != null) {
            return OperationType.SUBSCRIPTION;
        }
        return null;
    }

    public GraphqlAntlrManager getGraphqlAntlrManager() {
        return this.graphqlAntlrManager;
    }
}
