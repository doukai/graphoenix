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

    @Override
    public GraphqlParser.OperationTypeDefinitionContext getOperationTypeDefinition(String operationTypeName) {
        return operationTypeDefinitionMap.get(operationTypeName);
    }

    @Override
    public Map<String, GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinitions() {
        return operationTypeDefinitionMap;
    }
}
