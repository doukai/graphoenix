package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.*;

import static io.graphoenix.spi.constant.Hammurabi.EXCLUDE_INPUT;

public abstract class SubscriptionDataListener {

    private final IGraphQLDocumentManager manager;

    private final Map<String, List<String>> idMap = new HashMap<>();

    public SubscriptionDataListener() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
    }

    public boolean merged(JsonObject operation) {
        for (GraphqlParser.SelectionContext selectionContext : operationDefinitionContext.selectionSet().selection()) {
            if (selectionContext.field().arguments() != null) {
                GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getMutationOperationTypeName()
                        .map(name ->
                                manager.getField(name, selectionContext.field().name().getText())
                                        .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(name, selectionContext.field().name().getText())))
                        )
                        .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));

                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                String fieldName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText();
                if (operation.get(fieldName) != null) {
                    return merge(fieldTypeName, selectionContext.field().arguments(), operation.get(fieldName));
                }
            }
        }
        return false;
    }

    private boolean merge(String typeName, GraphqlParser.ArgumentsContext argumentsContext, JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            if (argumentsContext != null) {
                for (GraphqlParser.ArgumentContext argumentContext : argumentsContext.argument()) {
                    if (Arrays.stream(EXCLUDE_INPUT).noneMatch(name -> name.equals(argumentContext.name().getText()))) {
                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, argumentContext.name().getText())
                                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, argumentContext.name().getText())));
                        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                        JsonValue fieldJsonValue = jsonValue.asJsonObject().get(argumentContext.name().getText());
                        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                            for (JsonValue item : fieldJsonValue.asJsonArray()) {
                                return merge(fieldTypeName, argumentContext.valueWithVariable().objectValueWithVariable(), item);
                            }
                        } else {
                            return merge(fieldTypeName, argumentContext.valueWithVariable().objectValueWithVariable(), fieldJsonValue);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean merge(String typeName, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
            if (idFieldName.isPresent() && !jsonValue.asJsonObject().isNull(idFieldName.get())) {
                if (idMap.get(typeName) != null && idMap.get(typeName).contains(jsonValue.asJsonObject().getString(idFieldName.get()))) {
                    return true;
                }
            }
            if (objectValueWithVariableContext != null) {
                for (GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext : objectValueWithVariableContext.objectFieldWithVariable()) {
                    if (Arrays.stream(EXCLUDE_INPUT).noneMatch(name -> name.equals(objectFieldWithVariableContext.name().getText()))) {
                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, objectFieldWithVariableContext.name().getText())
                                .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, objectFieldWithVariableContext.name().getText())));
                        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                        JsonValue fieldJsonValue = jsonValue.asJsonObject().get(objectFieldWithVariableContext.name().getText());
                        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                            for (JsonValue item : fieldJsonValue.asJsonArray()) {
                                return merge(fieldTypeName, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(), item);
                            }
                        } else {
                            return merge(fieldTypeName, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(), fieldJsonValue);
                        }
                    }
                }
            }
        }
        return false;
    }

    public SubscriptionDataListener indexData(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue) {
        String subscriptionTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
        idMap.clear();
        indexData(subscriptionTypeName, operationDefinitionContext.selectionSet(), jsonValue);
        return this;
    }

    private void indexData(String typeName, GraphqlParser.SelectionSetContext selectionSetContext, JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
            if (idFieldName.isPresent() && !jsonValue.asJsonObject().isNull(idFieldName.get())) {
                idMap.computeIfAbsent(typeName, k -> new ArrayList<>());
                idMap.get(typeName).add(jsonValue.asJsonObject().getString(idFieldName.get()));
            }
            if (selectionSetContext != null) {
                for (GraphqlParser.SelectionContext selectionContext : selectionSetContext.selection()) {
                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                            .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));
                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String fieldName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText();
                    JsonValue fieldJsonValue = jsonValue.asJsonObject().get(fieldName);
                    if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                        for (JsonValue item : fieldJsonValue.asJsonArray()) {
                            indexData(fieldTypeName, selectionContext.field().selectionSet(), item);
                        }
                    } else {
                        indexData(fieldTypeName, selectionContext.field().selectionSet(), fieldJsonValue);
                    }
                }
            }
        }
    }

    public abstract SubscriptionDataListener indexFilter(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    public abstract boolean merged(JsonValue jsonValue);
}
