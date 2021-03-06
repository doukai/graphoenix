package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLObjectManager implements IGraphQLObjectManager {

    private final Map<String, GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.ObjectTypeDefinitionContext> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        objectTypeDefinitionMap.put(objectTypeDefinitionContext.name().getText(), objectTypeDefinitionContext);
        return objectTypeDefinitionMap;
    }

    @Override
    public boolean isObject(String objectTypeName) {
        return objectTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(objectTypeName));
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinition(String objectTypeName) {
        return objectTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinitions() {
        return objectTypeDefinitionMap.values().stream();
    }
}
