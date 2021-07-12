package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlEnumManager;

import java.util.HashMap;
import java.util.Map;

public class GraphqlEnumManager implements IGraphqlEnumManager {

    private final Map<String, GraphqlParser.EnumTypeDefinitionContext> enumTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        enumTypeDefinitionMap.put(enumTypeDefinitionContext.name().getText(), enumTypeDefinitionContext);
        return enumTypeDefinitionMap;
    }
}
