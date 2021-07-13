package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlInputValueManager;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Map<String, GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName) {
        return inputValueDefinitionMap.get(inputObjectTypeName);
    }
}
