package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlScalarManager;

import java.util.HashMap;
import java.util.Map;

public class GraphqlScalarManager implements IGraphqlScalarManager {

    private final Map<String, GraphqlParser.ScalarTypeDefinitionContext> scalarTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        scalarTypeDefinitionMap.put(scalarTypeDefinitionContext.name().getText(), scalarTypeDefinitionContext);
        return scalarTypeDefinitionMap;
    }
}
