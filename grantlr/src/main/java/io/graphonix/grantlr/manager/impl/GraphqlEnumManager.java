package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlEnumManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphqlEnumManager implements IGraphqlEnumManager {

    private final Map<String, GraphqlParser.EnumTypeDefinitionContext> enumTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        enumTypeDefinitionMap.put(enumTypeDefinitionContext.name().getText(), enumTypeDefinitionContext);
        return enumTypeDefinitionMap;
    }

    public GraphqlParser.EnumTypeDefinitionContext get(String name) {
        return enumTypeDefinitionMap.get(name);
    }

    @Override
    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinition(String enumTypeName) {
        return enumTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(enumTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinitions() {
        return enumTypeDefinitionMap.values().stream();
    }
}
