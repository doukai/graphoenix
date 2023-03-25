package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLOperationTypeManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLOperationTypeManager implements IGraphQLOperationTypeManager {

    private final Map<String, GraphqlParser.OperationTypeDefinitionContext> operationTypeDefinitionMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.OperationTypeDefinitionContext> register(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        operationTypeDefinitionMap.put(operationTypeDefinitionContext.typeName().name().getText(), operationTypeDefinitionContext);
        Logger.info("registered operationType {}", operationTypeDefinitionContext.typeName().name().getText());
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

    @Override
    public void clear() {
        operationTypeDefinitionMap.clear();
        Logger.debug("clear all operationType");
    }
}
