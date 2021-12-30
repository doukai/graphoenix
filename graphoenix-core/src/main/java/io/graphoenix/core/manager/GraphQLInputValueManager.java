package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInputValueManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphQLInputValueManager implements IGraphQLInputValueManager {

    private final Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> inputValueDefinitionTree = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputValueDefinitionTree.put(inputObjectTypeDefinitionContext.name().getText(),
                inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                        .collect(Collectors.toMap(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText(), inputValueDefinitionContext -> inputValueDefinitionContext)));
        return inputValueDefinitionTree;
    }

    @Override
    public Stream<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName) {
        return inputValueDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName)).map(Map.Entry::getValue)
                .flatMap(entry -> entry.values().stream());
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName, String inputValueName) {
        return inputValueDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName))
                .map(Map.Entry::getValue).findFirst()
                .flatMap(inputValueDefinitionMap -> inputValueDefinitionMap.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(inputValueName))
                        .map(Map.Entry::getValue).findFirst());
    }
}
