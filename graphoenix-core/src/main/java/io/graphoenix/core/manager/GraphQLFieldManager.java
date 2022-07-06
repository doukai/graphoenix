package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.*;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class GraphQLFieldManager implements IGraphQLFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new LinkedHashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap = new LinkedHashMap<>();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));
        fieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(), fieldMap);
        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap = new LinkedHashMap<>();
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));
        fieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(), fieldMap);
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
                .flatMap(fieldDefinitionMap ->
                        fieldDefinitionMap.entrySet().stream()
                                .filter(entry -> entry.getKey().equals(fieldName))
                                .map(Map.Entry::getValue)
                                .findFirst()
                );
    }

    @Override
    public Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitionByDirective(String objectTypeName, String directiveName) {
        return fieldDefinitionTree.entrySet().stream()
                .filter(entry -> entry.getKey().equals(objectTypeName))
                .flatMap(entry -> entry.getValue().values().stream())
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext -> directiveContext.name().getText().equals(directiveName))
                );
    }

    @Override
    public boolean isInvokeField(String objectTypeName, String fieldName) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = fieldDefinitionTree.get(objectTypeName).get(fieldName);
        return isInvokeField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotInvokeField(String objectTypeName, String fieldName) {
        return !isInvokeField(objectTypeName, fieldName);
    }

    @Override
    public boolean isFunctionField(String objectTypeName, String fieldName) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = fieldDefinitionTree.get(objectTypeName).get(fieldName);
        return isFunctionField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotFunctionField(String objectTypeName, String fieldName) {
        return !isFunctionField(objectTypeName, fieldName);
    }

    @Override
    public boolean isConnectionField(String objectTypeName, String fieldName) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = fieldDefinitionTree.get(objectTypeName).get(fieldName);
        return isConnectionField(fieldDefinitionContext);
    }

    @Override
    public boolean isNotConnectionField(String objectTypeName, String fieldName) {
        return !isConnectionField(objectTypeName, fieldName);
    }

    @Override
    public boolean isInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives() != null && fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> Arrays.stream(INVOKE_DIRECTIVES).anyMatch(name -> directiveContext.name().getText().equals(name)));
    }

    @Override
    public boolean isNotInvokeField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return !isInvokeField(fieldDefinitionContext);
    }

    @Override
    public boolean isFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives() != null && fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals(FUNC_DIRECTIVE_NAME));
    }

    @Override
    public boolean isNotFunctionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return !isFunctionField(fieldDefinitionContext);
    }

    @Override
    public boolean isConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives() != null && fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals(CONNECTION_DIRECTIVE_NAME));
    }

    @Override
    public boolean isNotConnectionField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return !isConnectionField(fieldDefinitionContext);
    }

    @Override
    public void clear() {
        fieldDefinitionTree.clear();
        Logger.debug("clear all field");
    }
}
