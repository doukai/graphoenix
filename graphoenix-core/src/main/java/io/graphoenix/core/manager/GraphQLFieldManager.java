package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphQLFieldManager implements IGraphQLFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        fieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));
        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        fieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(),
                interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));
        return fieldDefinitionTree;
    }

    @Override
    public Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitions(String objectTypeName) {
        return fieldDefinitionTree.entrySet().stream()
                .filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue)
                .flatMap(stringFieldDefinitionContextMap -> stringFieldDefinitionContextMap.values().stream());
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinition(String objectTypeName, String fieldName) {
        return fieldDefinitionTree.entrySet().stream()
                .filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue).findFirst()
                .flatMap(fieldDefinitionMap -> fieldDefinitionMap.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(fieldName))
                        .map(Map.Entry::getValue).findFirst());
    }

    @Override
    public void clear() {
        fieldDefinitionTree.clear();
    }
}
