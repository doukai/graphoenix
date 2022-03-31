package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLFieldManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.*;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.INVOKE_DIRECTIVES;

@ApplicationScoped
public class GraphQLFieldManager implements IGraphQLFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new LinkedHashMap<>();

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> invokeFieldDefinitionTree = new LinkedHashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap = new LinkedHashMap<>();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));
        fieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(), fieldMap);

        Map<String, GraphqlParser.FieldDefinitionContext> invokeFieldMap = new LinkedHashMap<>();
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(name -> directiveContext.name().getText().equals(name))
                                )
                )
                .forEach(fieldDefinitionContext -> {
                            invokeFieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext);
                            Logger.info("registered object {} field {}", objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
                        }
                );
        invokeFieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(), invokeFieldMap);
        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        Map<String, GraphqlParser.FieldDefinitionContext> fieldMap = new LinkedHashMap<>();
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().forEach(fieldDefinitionContext -> fieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext));
        fieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(), fieldMap);

        Map<String, GraphqlParser.FieldDefinitionContext> invokeFieldMap = new LinkedHashMap<>();
        interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(name -> directiveContext.name().getText().equals(name))
                                )
                )
                .forEach(fieldDefinitionContext -> {
                            invokeFieldMap.put(fieldDefinitionContext.name().getText(), fieldDefinitionContext);
                            Logger.info("registered interface {} field {}", interfaceTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
                        }
                );
        invokeFieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(), invokeFieldMap);
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
        return invokeFieldDefinitionTree.get(objectTypeName).containsKey(fieldName);
    }

    @Override
    public boolean isNotInvokeField(String objectTypeName, String fieldName) {
        return !isInvokeField(objectTypeName, fieldName);
    }

    @Override
    public boolean isFunctionField(String objectTypeName, String fieldName) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = fieldDefinitionTree.get(objectTypeName).get(fieldName);
        return fieldDefinitionContext.directives() != null && fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("func"));
    }

    @Override
    public boolean isNotFunctionField(String objectTypeName, String fieldName) {
        return !isFunctionField(objectTypeName, fieldName);
    }

    @Override
    public boolean isConnectionField(String objectTypeName, String fieldName) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = fieldDefinitionTree.get(objectTypeName).get(fieldName);
        return fieldDefinitionContext.directives() != null && fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("connection"));
    }

    @Override
    public boolean isNotConnectionField(String objectTypeName, String fieldName) {
        return !isConnectionField(objectTypeName, fieldName);
    }

    @Override
    public void clear() {
        fieldDefinitionTree.clear();
        invokeFieldDefinitionTree.clear();
        Logger.debug("clear all field");
    }
}
