package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlInputObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphqlInputObjectManager implements IGraphqlInputObjectManager {

    private final Map<String, GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputObjectTypeDefinitionMap.put(inputObjectTypeDefinitionContext.name().getText(), inputObjectTypeDefinitionContext);
        return inputObjectTypeDefinitionMap;
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
