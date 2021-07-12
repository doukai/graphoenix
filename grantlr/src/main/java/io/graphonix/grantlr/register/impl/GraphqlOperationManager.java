package io.graphonix.grantlr.register.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.register.IGraphqlOperationManager;

import java.util.HashMap;
import java.util.Map;

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

}
