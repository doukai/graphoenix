package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlOperationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GraphqlOperationManager implements IGraphqlOperationManager {

    private final Map<String, GraphqlParser.OperationTypeDefinitionContext> operationTypeDefinitionMap = new HashMap<>();

    public Map<String, GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinitionMap() {
        return operationTypeDefinitionMap;
    }

    @Override
    public Map<String, GraphqlParser.OperationTypeDefinitionContext> register(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        operationTypeDefinitionMap.put(operationTypeDefinitionContext.typeName().name().getText(), operationTypeDefinitionContext);
        return operationTypeDefinitionMap;
    }

    public Optional<Map.Entry<String, GraphqlParser.OperationTypeDefinitionContext>> getQueryOperationTypeDefinition() {
        return operationTypeDefinitionMap.entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().QUERY() != null).findFirst();
    }

    public Optional<Map.Entry<String, GraphqlParser.OperationTypeDefinitionContext>> getMutationOperationTypeDefinition() {
        return operationTypeDefinitionMap.entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().MUTATION() != null).findFirst();
    }

    public Optional<Map.Entry<String, GraphqlParser.OperationTypeDefinitionContext>> getSubscriptionOperationTypeDefinition() {
        return operationTypeDefinitionMap.entrySet().stream()
                .filter(operationTypeDefinitionContextEntry -> operationTypeDefinitionContextEntry.getValue().operationType().SUBSCRIPTION() != null).findFirst();
    }
}
