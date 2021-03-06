package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLOperationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLOperationManager implements IGraphQLOperationManager {

    private final Map<String, GraphqlParser.OperationTypeDefinitionContext> operationTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.OperationTypeDefinitionContext> register(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        operationTypeDefinitionMap.put(operationTypeDefinitionContext.typeName().name().getText(), operationTypeDefinitionContext);
        return operationTypeDefinitionMap;
    }

    @Override
    public boolean isOperation(String operationTypeName) {
        return operationTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(operationTypeName));
    }

    @Override
    public Optional<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinition(String operationTypeName) {
        return operationTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(operationTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinitions() {
        return operationTypeDefinitionMap.values().stream();
    }
}
