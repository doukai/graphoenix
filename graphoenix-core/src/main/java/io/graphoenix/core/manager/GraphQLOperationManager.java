package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLOperationManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLOperationManager implements IGraphQLOperationManager {

    private final Map<String, GraphqlParser.OperationDefinitionContext> operationDefinitionMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.OperationDefinitionContext> register(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        operationDefinitionMap.put(operationDefinitionContext.name().getText(), operationDefinitionContext);
        Logger.info("registered operation {}", operationDefinitionContext.name().getText());
        return operationDefinitionMap;
    }

    @Override
    public Optional<GraphqlParser.OperationDefinitionContext> getOperationDefinition(String operationTypeName) {
        return operationDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(operationTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.OperationDefinitionContext> getOperationDefinitions() {
        return operationDefinitionMap.values().stream();
    }

    @Override
    public void clear() {
        operationDefinitionMap.clear();
        Logger.debug("clear all operation");
    }
}
