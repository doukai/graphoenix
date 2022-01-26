package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.INVOKE_DIRECTIVES;

public class GraphQLFieldManager implements IGraphQLFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new HashMap<>();

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> invokeFieldDefinitionTree = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap;
        if (fieldDefinitionTree.get(objectTypeDefinitionContext.name().getText()) == null) {
            fieldMap = new HashMap<>();
        } else {
            fieldMap = fieldDefinitionTree.get(objectTypeDefinitionContext.name().getText());
        }
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));

        Map<String, GraphqlParser.FieldDefinitionContext> invokeFieldMap;
        if (invokeFieldDefinitionTree.get(objectTypeDefinitionContext.name().getText()) == null) {
            invokeFieldMap = new HashMap<>();
        } else {
            invokeFieldMap = invokeFieldDefinitionTree.get(objectTypeDefinitionContext.name().getText());
        }
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(name -> directiveContext.name().getText().equals(name))
                                )
                )
                .forEach(fieldDefinitionContext -> invokeFieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));

        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap;
        if (fieldDefinitionTree.get(interfaceTypeDefinitionContext.name().getText()) == null) {
            fieldMap = new HashMap<>();
        } else {
            fieldMap = fieldDefinitionTree.get(interfaceTypeDefinitionContext.name().getText());
        }
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));

        Map<String, GraphqlParser.FieldDefinitionContext> invokeFieldMap;
        if (invokeFieldDefinitionTree.get(interfaceTypeDefinitionContext.name().getText()) == null) {
            invokeFieldMap = new HashMap<>();
        } else {
            invokeFieldMap = invokeFieldDefinitionTree.get(interfaceTypeDefinitionContext.name().getText());
        }
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(name -> directiveContext.name().getText().equals(name))
                                )
                )
                .forEach(fieldDefinitionContext -> invokeFieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));

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
    public boolean isInvokeField(String objectTypeName, String fieldName) {
        return invokeFieldDefinitionTree.get(objectTypeName).containsKey(fieldName);
    }

    @Override
    public boolean isNotInvokeField(String objectTypeName, String fieldName) {
        return !isInvokeField(objectTypeName, fieldName);
    }

    @Override
    public void clear() {
        fieldDefinitionTree.clear();
        invokeFieldDefinitionTree.clear();
    }
}
