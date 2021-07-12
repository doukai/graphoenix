package io.graphonix.grantlr.register.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.register.IGraphqlFieldManager;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphqlFieldManager implements IGraphqlFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionMap = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {

        fieldDefinitionMap.put(objectTypeDefinitionContext.name().getText(),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));
        return fieldDefinitionMap;
    }
}
