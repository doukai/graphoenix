package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequestScoped
public class SubscriptionDataListener {

    private final IGraphQLDocumentManager manager;

    private final JsonProvider jsonProvider;

    private JsonObject data;

    private final Map<String, Map<String, List<Map.Entry<String, JsonObject>>>> idIndexMap = new HashMap<>();

    @Inject
    public SubscriptionDataListener(IGraphQLDocumentManager manager, JsonProvider jsonProvider) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
    }

    public Mono<JsonValue> merge(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue) {
        if (data == null) {
            String subscriptionTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
            indexData("", subscriptionTypeName, operationDefinitionContext.selectionSet(), jsonValue.asJsonObject());
            data = jsonValue.asJsonObject();
            return Mono.just(jsonValue);
        } else {
            JsonObject jsonObject = jsonValue.asJsonObject();
            String typeName = jsonObject.getString("type");
            JsonValue mutation = jsonObject.get("mutation");
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
            if (idFieldName.isPresent()) {
                if (mutation.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                    for (JsonValue item : mutation.asJsonArray()) {
                        merge(typeName, item.asJsonObject());
                    }
                } else {
                    merge(typeName, mutation.asJsonObject());
                }
            }
        }
        return Mono.empty();
    }

    public void merge(String typeName, JsonObject jsonObject) {
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        if (idFieldName.isPresent()) {
            String id = jsonObject.getString(idFieldName.get());
            if (idIndexMap.get(typeName) != null && idIndexMap.get(typeName).get(id) != null) {
                for (Map.Entry<String, JsonObject> entry : idIndexMap.get(typeName).get(id)) {
                    JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
                    JsonMergePatch mergePatch = jsonProvider.createMergePatch(jsonObject);
                    patchBuilder.replace(entry.getKey(), mergePatch.apply(entry.getValue()));
                }
            }
        }
    }

    private void indexData(String path, String typeName, GraphqlParser.SelectionSetContext selectionSetContext, JsonObject jsonObject) {
        for (GraphqlParser.SelectionContext selectionContext : selectionSetContext.selection()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                String fieldName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText();
                if (idFieldName.isPresent() && !jsonObject.isNull(fieldName)) {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                        JsonArray jsonArray = jsonObject.getJsonArray(fieldName);
                        for (int i = 0; i < jsonArray.size(); i++) {
                            indexData(path + "/" + i, typeName, selectionSetContext, jsonArray.getJsonObject(i));
                        }
                    } else {
                        String id = jsonObject.getString(idFieldName.get());
                        idIndexMap.computeIfAbsent(fieldTypeName, k -> new HashMap<>());
                        idIndexMap.get(fieldTypeName).computeIfAbsent(id, k -> new ArrayList<>());
                        JsonObject field = jsonObject.getJsonObject(fieldName);
                        idIndexMap.get(fieldTypeName).get(id).add(new AbstractMap.SimpleEntry<>(path + "/" + fieldName, field));
                        indexData(path + "/" + fieldName, fieldTypeName, selectionContext.field().selectionSet(), field);
                    }
                }
            }
        }
    }
}
