package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInputObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLInputObjectManager implements IGraphQLInputObjectManager {

    private final Map<String, GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputObjectTypeDefinitionMap.put(inputObjectTypeDefinitionContext.name().getText(), inputObjectTypeDefinitionContext);
        return inputObjectTypeDefinitionMap;
    }

    @Override
    public boolean isInputObject(String inputObjectName) {
        return inputObjectTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(inputObjectName));
    }

    @Override
    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinition(String inputObjectName) {
        return inputObjectTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinitions() {
        return inputObjectTypeDefinitionMap.values().stream();
    }
}
