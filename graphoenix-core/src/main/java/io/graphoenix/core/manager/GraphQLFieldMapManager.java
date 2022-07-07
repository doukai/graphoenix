package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.map.FieldMap;
import io.graphoenix.spi.dto.map.FieldMapWith;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;

@ApplicationScoped
public class GraphQLFieldMapManager implements IGraphQLFieldMapManager {

    private final IGraphQLDocumentManager manager;

    private final Map<String, Map<String, FieldMap>> fieldMapTree = new LinkedHashMap<>();

    @Inject
    public GraphQLFieldMapManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Override
    public void registerFieldMaps() {
        manager.getObjects()
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .forEach(objectTypeDefinitionContext ->
                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                .filter(fieldDefinitionContext -> manager.isNotConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                .filter(fieldDefinitionContext -> manager.isNotInvokeField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                .filter(fieldDefinitionContext -> manager.isNotFunctionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()))
                                .forEach(fieldDefinitionContext -> {
                                            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())) ||
                                                    (manager.fieldTypeIsList(fieldDefinitionContext.type()) && manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) ||
                                                    (manager.fieldTypeIsList(fieldDefinitionContext.type()) && manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                            ) {
                                                if (fieldDefinitionContext.directives() == null) {
                                                    throw new GraphQLErrors(MAP_DIRECTIVE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                }
                                                Optional<GraphqlParser.DirectiveContext> mapDirective = fieldDefinitionContext.directives().directive().stream()
                                                        .filter(directiveContext -> directiveContext.name().getText().equals("map"))
                                                        .findFirst();

                                                if (mapDirective.isEmpty()) {
                                                    throw new GraphQLErrors(MAP_DIRECTIVE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                } else {
                                                    Optional<GraphqlParser.ArgumentContext> fromArgument = mapDirective.get().arguments().argument().stream()
                                                            .filter(argumentContext -> argumentContext.name().getText().equals("from"))
                                                            .findFirst();

                                                    if (fromArgument.isEmpty()) {
                                                        throw new GraphQLErrors(MAP_FROM_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                    }

                                                    Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getField(
                                                            objectTypeDefinitionContext.name().getText(),
                                                            DOCUMENT_UTIL.getStringValue(fromArgument.get().valueWithVariable().StringValue())
                                                    ).or(() ->
                                                            manager.getInterface("Meta").stream()
                                                                    .flatMap(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                                                                    .filter(metaFieldDefinitionContext -> metaFieldDefinitionContext.name().getText().equals(DOCUMENT_UTIL.getStringValue(fromArgument.get().valueWithVariable().StringValue())))
                                                                    .findFirst()
                                                    );

                                                    if (fromFieldDefinition.isEmpty()) {
                                                        throw new GraphQLErrors(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                    }

                                                    Boolean anchor = mapDirective.get().arguments().argument().stream()
                                                            .filter(argumentContext -> argumentContext.name().getText().equals("anchor"))
                                                            .filter(argumentContext -> argumentContext.valueWithVariable().BooleanValue() != null)
                                                            .findFirst()
                                                            .map(argumentContext -> Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()))
                                                            .orElse(false);

                                                    Optional<GraphqlParser.ArgumentContext> withArgument = mapDirective.get().arguments().argument()
                                                            .stream()
                                                            .filter(argumentContext -> argumentContext.name().getText().equals("with"))
                                                            .findFirst();

                                                    if (withArgument.isPresent()) {

                                                        Optional<GraphqlParser.ObjectFieldWithVariableContext> withTypeArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("type"))
                                                                .findFirst();

                                                        if (withTypeArgument.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_TYPE_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        Optional<GraphqlParser.ObjectFieldWithVariableContext> withFromArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("from"))
                                                                .findFirst();

                                                        if (withFromArgument.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_FROM_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        Optional<GraphqlParser.ObjectFieldWithVariableContext> withToArgument = withArgument.get().valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("to"))
                                                                .findFirst();

                                                        if (withToArgument.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_TO_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        Optional<GraphqlParser.ObjectTypeDefinitionContext> withObjectTypeDefinition = manager.getObject(DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue()));

                                                        if (withObjectTypeDefinition.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        Optional<GraphqlParser.FieldDefinitionContext> withFromFieldDefinition = manager.getField(
                                                                DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue()),
                                                                DOCUMENT_UTIL.getStringValue(withFromArgument.get().valueWithVariable().StringValue())
                                                        ).or(() ->
                                                                manager.getInterface("Meta").stream()
                                                                        .flatMap(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                                                                        .filter(metaFieldDefinitionContext -> metaFieldDefinitionContext.name().getText().equals(DOCUMENT_UTIL.getStringValue(withFromArgument.get().valueWithVariable().StringValue())))
                                                                        .findFirst()
                                                        );

                                                        if (withFromFieldDefinition.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        Optional<GraphqlParser.FieldDefinitionContext> withToFieldDefinition = manager.getField(
                                                                DOCUMENT_UTIL.getStringValue(withTypeArgument.get().valueWithVariable().StringValue()),
                                                                DOCUMENT_UTIL.getStringValue(withToArgument.get().valueWithVariable().StringValue())
                                                        ).or(() ->
                                                                manager.getInterface("Meta").stream()
                                                                        .flatMap(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                                                                        .filter(metaFieldDefinitionContext -> metaFieldDefinitionContext.name().getText().equals(DOCUMENT_UTIL.getStringValue(withToArgument.get().valueWithVariable().StringValue())))
                                                                        .findFirst()
                                                        );

                                                        if (withToFieldDefinition.isEmpty()) {
                                                            throw new GraphQLErrors(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                                                        }

                                                        registerMap(objectTypeDefinitionContext.name().getText(),
                                                                fieldDefinitionContext.name().getText(),
                                                                fromFieldDefinition.get(),
                                                                anchor,
                                                                withObjectTypeDefinition.get(),
                                                                withFromFieldDefinition.get(),
                                                                withToFieldDefinition.get(),
                                                                getToFieldDefinition(mapDirective.get(), fieldDefinitionContext));

                                                    } else {

                                                        registerMap(objectTypeDefinitionContext.name().getText(),
                                                                fieldDefinitionContext.name().getText(),
                                                                fromFieldDefinition.get(),
                                                                anchor,
                                                                getToFieldDefinition(mapDirective.get(), fieldDefinitionContext));
                                                    }
                                                }
                                            }
                                        }
                                )
                );
    }

    private GraphqlParser.FieldDefinitionContext getToFieldDefinition(GraphqlParser.DirectiveContext mapDirective, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            Optional<GraphqlParser.ArgumentContext> toArgument = mapDirective.arguments().argument()
                    .stream().filter(argumentContext -> argumentContext.name().getText().equals("to")).findFirst();
            if (toArgument.isEmpty()) {
                throw new GraphQLErrors(MAP_TO_ARGUMENT_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getField(
                    manager.getFieldTypeName(fieldDefinitionContext.type()),
                    DOCUMENT_UTIL.getStringValue(toArgument.get().valueWithVariable().StringValue())
            ).or(() ->
                    manager.getInterface("Meta").stream()
                            .flatMap(interfaceTypeDefinitionContext -> interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                            .filter(metaFieldDefinitionContext -> metaFieldDefinitionContext.name().getText().equals(DOCUMENT_UTIL.getStringValue(toArgument.get().valueWithVariable().StringValue())))
                            .findFirst()
            );
            if (toFieldDefinition.isEmpty()) {
                throw new GraphQLErrors(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            return toFieldDefinition.get();
        }
        return null;
    }


    @Override
    public void registerMap(String objectTypeName,
                            String fieldName,
                            GraphqlParser.FieldDefinitionContext from,
                            Boolean anchor,
                            GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, anchor, to));
        fieldMapTree.put(objectTypeName, fieldMap);
        Logger.info("map {}.{} from {}.{} to {}.{}",
                objectTypeName,
                fieldName,
                objectTypeName,
                from.name().getText(),
                manager.getFieldTypeName(to.type()),
                to.name().getText()
        );
    }

    @Override
    public void registerMap(String objectTypeName,
                            String fieldName,
                            GraphqlParser.FieldDefinitionContext from,
                            Boolean anchor,
                            GraphqlParser.ObjectTypeDefinitionContext withType,
                            GraphqlParser.FieldDefinitionContext withFrom,
                            GraphqlParser.FieldDefinitionContext withTo,
                            GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, anchor, new FieldMapWith(withType, withFrom, withTo), to));
        fieldMapTree.put(objectTypeName, fieldMap);
        Logger.info("map {}.{} from {}.{} with {}.{} and {}.{} to {}.{}",
                objectTypeName,
                fieldName,
                objectTypeName,
                from.name().getText(),
                withType.name().getText(),
                withFrom.name().getText(),
                withType.name().getText(),
                withTo.name().getText(),
                to != null ? manager.getFieldTypeName(to.type()) : null,
                to != null ? to.name().getText() : null
        );
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
    public Optional<GraphqlParser.FieldDefinitionContext> getToFieldDefinition(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getTo);
    }

    @Override
    public boolean anchor(String objectTypeName, String fieldName) {
        return getFieldMap(objectTypeName, fieldName).map(FieldMap::getAnchor).orElse(false);
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
                                .filter(argumentContext -> argumentContext.valueWithVariable().NullValue() == null)
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
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fromFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                                .findFirst()
                );
    }

    @Override
    public Optional<GraphqlParser.ValueContext> getMapFromValueFromObjectField(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.ObjectValueContext parentObjectValueContext) {
        return getFromFieldDefinition(manager.getFieldTypeName(parentFieldDefinitionContext.type()), fieldDefinitionContext.name().getText())
                .flatMap(fromFieldDefinition ->
                        parentObjectValueContext.objectField().stream()
                                .filter(objectFieldContext -> objectFieldContext.value().NullValue() == null)
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
                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().NullValue() == null)
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
                                .filter(objectFieldContext -> objectFieldContext.value().NullValue() == null)
                                .filter(objectFieldContext -> objectFieldContext.name().getText().equals(toFieldDefinition.name().getText()))
                                .map(GraphqlParser.ObjectFieldContext::value)
                                .findFirst()
                );
    }

    @Override
    public void clear() {
        fieldMapTree.clear();
        Logger.debug("clear field map");
    }
}
