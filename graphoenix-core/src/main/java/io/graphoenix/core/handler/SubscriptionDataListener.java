package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;

import java.util.*;

public abstract class SubscriptionDataListener {

    private final IGraphQLDocumentManager manager;

    private final Map<String, List<String>> idMap = new HashMap<>();

    public SubscriptionDataListener() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
    }

    public boolean merged(String typeName, JsonArray arguments) {
        for (JsonValue argument : arguments) {
            if (merged(typeName, argument)) {
                return true;
            }
        }
        return false;
    }

    private boolean merged(String typeName, JsonValue argument) {
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        if (idFieldName.isPresent() && argument.asJsonObject().containsKey(idFieldName.get()) && !argument.asJsonObject().isNull(idFieldName.get())) {
            if (idMap.get(typeName) != null && idMap.get(typeName).contains(argument.asJsonObject().getString(idFieldName.get()))) {
                return true;
            }
        }
        for (Map.Entry<String, JsonValue> entry : argument.asJsonObject().entrySet()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, entry.getKey())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, entry.getKey())));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (manager.isObject(fieldTypeName)) {
                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                    for (JsonValue item : entry.getValue().asJsonArray()) {
                        if (merged(fieldTypeName, item)) {
                            return true;
                        }
                    }
                } else {
                    if (merged(fieldTypeName, entry.getValue())) {
                        return true;
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
