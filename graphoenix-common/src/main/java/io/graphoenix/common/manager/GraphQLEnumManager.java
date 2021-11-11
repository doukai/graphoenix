package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLEnumManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLEnumManager implements IGraphQLEnumManager {

    private final Map<String, GraphqlParser.EnumTypeDefinitionContext> enumTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        enumTypeDefinitionMap.put(enumTypeDefinitionContext.name().getText(), enumTypeDefinitionContext);
        return enumTypeDefinitionMap;
    }

    @Override
    public boolean isEnum(String enumTypeName) {
        return enumTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(enumTypeName));
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
