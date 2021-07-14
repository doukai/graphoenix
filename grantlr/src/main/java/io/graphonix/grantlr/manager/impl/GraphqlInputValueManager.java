package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlInputValueManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlInputValueManager implements IGraphqlInputValueManager {

    private final Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> inputValueDefinitionMap = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputValueDefinitionMap.put(inputObjectTypeDefinitionContext.name().getText(),
                inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                        .collect(Collectors.toMap(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText(), inputValueDefinitionContext -> inputValueDefinitionContext)));
        return inputValueDefinitionMap;
    }

    @Override
    public Stream<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName) {
        return inputValueDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName)).map(Map.Entry::getValue)
                .flatMap(entry -> entry.values().stream());
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName, String inputValueName) {
        return inputValueDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName)).map(Map.Entry::getValue).findFirst()
                .flatMap(entry -> entry.values().stream().findFirst());
    }
}
