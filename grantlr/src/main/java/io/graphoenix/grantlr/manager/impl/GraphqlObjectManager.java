package io.graphoenix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.grantlr.manager.IGraphqlObjectManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlObjectManager implements IGraphqlObjectManager {

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

    @Override
    public List<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinitionList() {
        return getObjectTypeDefinitions().collect(Collectors.toList());
    }
}
