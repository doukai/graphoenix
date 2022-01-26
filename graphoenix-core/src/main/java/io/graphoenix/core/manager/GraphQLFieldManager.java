package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.INVOKE_DIRECTIVES;

public class GraphQLFieldManager implements IGraphQLFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new ConcurrentHashMap<>();

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> invokeFieldDefinitionTree = new ConcurrentHashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        fieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(),
                new HashSet<>(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()).stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));

        invokeFieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(),
                new HashSet<>(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()).stream()
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext ->
                                fieldDefinitionContext.directives().directive().stream()
                                        .anyMatch(directiveContext ->
                                                Arrays.stream(INVOKE_DIRECTIVES)
                                                        .anyMatch(name -> directiveContext.name().getText().equals(name))
                                        )
                        )
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));

        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        fieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(),
                new HashSet<>(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()).stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));

        invokeFieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(),
                new HashSet<>(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition()).stream()
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext ->
                                fieldDefinitionContext.directives().directive().stream()
                                        .anyMatch(directiveContext ->
                                                Arrays.stream(INVOKE_DIRECTIVES)
                                                        .anyMatch(name -> directiveContext.name().getText().equals(name))
                                        )
                        )
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
