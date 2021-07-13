package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlObjectManager;

import java.util.HashMap;
import java.util.Map;

public class GraphqlObjectManager implements IGraphqlObjectManager {

    private final Map<String, GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.ObjectTypeDefinitionContext> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        objectTypeDefinitionMap.put(objectTypeDefinitionContext.name().getText(), objectTypeDefinitionContext);
        return objectTypeDefinitionMap;
    }

    @Override
    public GraphqlParser.ObjectTypeDefinitionContext getObjectTypeDefinition(String objectTypeName) {
        return objectTypeDefinitionMap.get(objectTypeName);
    }
}