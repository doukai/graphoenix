package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.common.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.map.FieldMap;
import io.graphoenix.spi.dto.map.FieldMapWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.error.GraphQLErrorType.*;
import static io.graphoenix.spi.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;

public class GraphQLFieldMapManager implements IGraphQLFieldMapManager {

    private final IGraphQLDocumentManager manager;

    private final Map<String, Map<String, FieldMap>> fieldMapTree = new HashMap<>();

    public GraphQLFieldMapManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
        this.registerFieldMaps();
    }

    @Override
    public void registerFieldMaps() {
        manager.getObjects().forEach(
                objectTypeDefinitionContext ->
                        manager.getFields(
                                objectTypeDefinitionContext.name().getText()
                        ).forEach(
                                fieldDefinitionContext -> {
                                    if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                        if (fieldDefinitionContext.directives() == null) {
                                            throw new GraphQLProblem(MAP_DIRECTIVE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                        }
                                        Optional<GraphqlParser.DirectiveContext> mapDirective = fieldDefinitionContext.directives().directive().stream()
                                                .filter(directiveContext -> directiveContext.name().getText().equals("map"))
                                                .findFirst();
                                        if (mapDirective.isEmpty()) {
                                            throw new GraphQLProblem(MAP_DIRECTIVE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                        } else {
                                            Optional<GraphqlParser.ArgumentContext> fromArgument = mapDirective.get().arguments().argument()
                                                    .stream().filter(argumentContext -> argumentContext.name().getText().equals("from")).findFirst();
                                            if (fromArgument.isEmpty()) {
                                                throw new GraphQLProblem(MAP_FROM_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                            }
                                            Optional<GraphqlParser.ArgumentContext> toArgument = mapDirective.get().arguments().argument()
                                                    .stream().filter(argumentContext -> argumentContext.name().getText().equals("to")).findFirst();
                                            if (toArgument.isEmpty()) {
                                                throw new GraphQLProblem(MAP_TO_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                            }
                                            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getField(
                                                    objectTypeDefinitionContext.name().getText(),
                                                    DOCUMENT_UTIL.getStringValue(fromArgument.get().valueWithVariable().StringValue())
                                            );
                                            if (fromFieldDefinition.isEmpty()) {
                                                throw new GraphQLProblem(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                            }
                                            Optional<GraphqlParser.ObjectTypeDefinitionContext> toObjectTypeDefinition = manager.getObject(
                                                    manager.getFieldTypeName(fieldDefinitionContext.type())
                                            );
                                            if (toObjectTypeDefinition.isEmpty()) {
                                                throw new GraphQLProblem(MAP_TO_OBJECT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                            }
                                            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getField(
                                                    manager.getFieldTypeName(fieldDefinitionContext.type()),
                                                    DOCUMENT_UTIL.getStringValue(toArgument.get().valueWithVariable().StringValue())
                                            );
                                            if (toFieldDefinition.isEmpty()) {
                                                throw new GraphQLProblem(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                            }
                                            Optional<GraphqlParser.ArgumentContext> withArgument = mapDirective.get().arguments().argument()
                                                    .stream().filter(argumentContext -> argumentContext.name().getText().equals("with")).findFirst();

                                            if (withArgument.isPresent()) {
                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> withTypeArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                        .filter(
                                                                objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("type")
                                                        ).findFirst();
                                                if (withTypeArgument.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_TYPE_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> withFromArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                        .filter(
                                                                objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("from")
                                                        ).findFirst();
                                                if (withFromArgument.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_FROM_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> withToArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                        .filter(
                                                                objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("to")
                                                        ).findFirst();
                                                if (withToArgument.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_TO_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.ObjectTypeDefinitionContext> withObjectTypeDefinition = manager.getObject(
                                                        DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue())
                                                );
                                                if (withObjectTypeDefinition.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.FieldDefinitionContext> withFromFieldDefinition = manager.getField(
                                                        DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue()),
                                                        DOCUMENT_UTIL.getStringValue(withFromArgument.get().valueWithVariable().StringValue())
                                                );
                                                if (withFromFieldDefinition.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.FieldDefinitionContext> withToFieldDefinition = manager.getField(
                                                        DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue()),
                                                        DOCUMENT_UTIL.getStringValue(withToArgument.get().valueWithVariable().StringValue())
                                                );
                                                if (withToFieldDefinition.isEmpty()) {
                                                    throw new GraphQLProblem(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                registerMap(objectTypeDefinitionContext.name().getText(),
                                                        fieldDefinitionContext.name().getText(),
                                                        fromFieldDefinition.get(),
                                                        withObjectTypeDefinition.get(),
                                                        withFromFieldDefinition.get(),
                                                        withToFieldDefinition.get(),
                                                        toObjectTypeDefinition.get(),
                                                        toFieldDefinition.get());

                                            } else {
                                                registerMap(objectTypeDefinitionContext.name().getText(),
                                                        fieldDefinitionContext.name().getText(),
                                                        fromFieldDefinition.get(),
                                                        toObjectTypeDefinition.get(),
                                                        toFieldDefinition.get());

                                            }
                                        }
                                    }
                                }
                        )
        );
    }


    @Override
    public Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                          String fieldName,
                                                          GraphqlParser.FieldDefinitionContext from,
                                                          GraphqlParser.ObjectTypeDefinitionContext toType,
                                                          GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, toType, to));
        fieldMapTree.put(objectTypeName, fieldMap);
        return fieldMapTree;
    }

    @Override
    public Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                          String fieldName,
                                                          GraphqlParser.FieldDefinitionContext from,
                                                          GraphqlParser.ObjectTypeDefinitionContext withType,
                                                          GraphqlParser.FieldDefinitionContext withFrom,
                                                          GraphqlParser.FieldDefinitionContext withTo,
                                                          GraphqlParser.ObjectTypeDefinitionContext toType,
                                                          GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, new FieldMapWith(withType, withFrom, withTo), toType, to));
        fieldMapTree.put(objectTypeName, fieldMap);
        return fieldMapTree;
    }

    private Optional<FieldMap> getFieldMap(String objectTypeName, String fieldName) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            return Optional.empty();
        }
        FieldMap map = fieldMap.get(fieldName);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.of(map);
    }

    private Optional<FieldMapWith> getFieldMapWith(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getWith);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getFromFieldDefinition(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getFrom);
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getToObjectTypeDefinition(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getToType);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getToFieldDefinition(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getTo);
    }

    @Override
    public boolean mapWithType(String objectTypeName, String fieldName) {
        return getFieldMapWith(objectTypeName, fieldName).isPresent();
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getWithObjectTypeDefinition(String objectTypeName, String fieldName) {
        return getFieldMapWith(objectTypeName, fieldName).map(FieldMapWith::getType);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getWithFromFieldDefinition(String objectTypeName, String fieldName) {
        return getFieldMapWith(objectTypeName, fieldName).map(FieldMapWith::getFrom);
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getWithToFieldDefinition(String objectTypeName, String fieldName) {
        return getFieldMapWith(objectTypeName, fieldName).map(FieldMapWith::getTo);
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromArguments(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                     GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                     GraphqlParser.ArgumentsContext parentArgumentsContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(fromFieldDefinition ->
                        parentArgumentsContext.argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals(fromFieldDefinition.name().getText()))
                                .map(GraphqlParser.ArgumentContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapFromValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                   GraphqlParser.ObjectValueWithVariableContext parentObjectValueWithVariableContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(fromFieldDefinition ->
                        parentObjectValueWithVariableContext.objectFieldWithVariable().stream()
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fromFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                                .findFirst());
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getMapFromValueFromObjectField(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.ObjectValueContext parentObjectValueContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(fromFieldDefinition ->
                        parentObjectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(fromFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldContext::value)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueWithVariableContext> getMapToValueWithVariableFromObjectFieldWithVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                                                 GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(toFieldDefinition ->
                        objectValueWithVariableContext.objectFieldWithVariable().stream()
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(toFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getMapToValueFromObjectField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ObjectValueContext objectValueContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(fieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(toFieldDefinition ->
                        objectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(toFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldContext::value)
                                .findFirst()
                );
    }
}
