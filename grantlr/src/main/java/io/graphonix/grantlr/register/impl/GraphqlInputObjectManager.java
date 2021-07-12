package io.graphonix.grantlr.register.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.register.IGraphqlInputObjectManager;

import java.util.HashMap;
import java.util.Map;

public class GraphqlInputObjectManager implements IGraphqlInputObjectManager {

    private final Map<String, GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputObjectTypeDefinitionMap.put(inputObjectTypeDefinitionContext.name().getText(), inputObjectTypeDefinitionContext);
        return inputObjectTypeDefinitionMap;
    }
}
