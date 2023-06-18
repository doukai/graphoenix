package io.graphoenix.jsonpath.handler;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JakartaJsonProvider;
import com.jayway.jsonpath.spi.mapper.JakartaMappingProvider;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.jsonpath.expression.Expression;
import io.graphoenix.jsonpath.translator.GraphQLArgumentsToFilter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.SubscriptionDataListener;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.util.*;

import static io.graphoenix.spi.constant.Hammurabi.CURSOR_DIRECTIVE_NAME;

@RequestScoped
public class JsonPathSubscriptionDataListener implements SubscriptionDataListener {

    private final IGraphQLDocumentManager manager;

    private final JsonProvider jsonProvider;

    private final GraphQLArgumentsToFilter argumentsToFilter;

    private final Configuration configuration;

    private JsonObject data;

    private final Map<String, List<Map.Entry<String, String>>> objectMap = new HashMap<>();

    private final Map<String, List<Map.Entry<String, String>>> arrayObjectMap = new HashMap<>();

    private final Map<String, List<Map.Entry<String, String>>> connectionMap = new HashMap<>();

    @Inject
    public JsonPathSubscriptionDataListener(IGraphQLDocumentManager manager, JsonProvider jsonProvider, GraphQLArgumentsToFilter argumentsToFilter) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
        this.argumentsToFilter = argumentsToFilter;
        this.configuration = Configuration.builder()
                .jsonProvider(new JakartaJsonProvider())
                .mappingProvider(new JakartaMappingProvider())
                .options(EnumSet.noneOf(Option.class))
                .build();
    }

    @Override
    public Mono<JsonValue> merge(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue) {
        boolean merged = false;
        if (data == null) {
            String subscriptionTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.SUBSCRIBE_TYPE_NOT_EXIST));
            indexFilter(subscriptionTypeName, operationDefinitionContext.selectionSet());
            data = jsonValue.asJsonObject();
            return Mono.just(jsonValue);
        } else {
            JsonObject jsonObject = jsonValue.asJsonObject();
            String typeName = jsonObject.getString("type");
            JsonValue mutation = jsonObject.get("mutation");
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
            if (idFieldName.isPresent()) {
                DocumentContext documentContext = JsonPath.using(configuration).parse(mutation.asJsonArray());
                merged = merge(typeName, documentContext);
            }
        }
        if (merged) {
            return Mono.just(data);
        }
        return Mono.empty();
    }

    private boolean merge(String typeName, DocumentContext documentContext) {
        boolean merged = false;
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        if (idFieldName.isPresent()) {
            JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
            if (objectMap.get(typeName) != null) {
                for (Map.Entry<String, String> entry : objectMap.get(typeName)) {
                    List<JsonValue> jsonValueList = documentContext.read(entry.getValue());
                    JsonValue fieldJsonValue = data.get(entry.getKey());
                    if (jsonValueList.size() > 0) {
                        merged = true;
                        if (fieldJsonValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                            patchBuilder.add("/" + entry.getKey(), jsonValueList.get(0));
                        } else {
                            if (fieldJsonValue.asJsonObject().getString(idFieldName.get()).equals(jsonValueList.get(0).asJsonObject().getString(idFieldName.get()))) {
                                JsonMergePatch mergePatch = jsonProvider.createMergePatch(jsonValueList.get(0));
                                patchBuilder.replace("/" + entry.getKey(), mergePatch.apply(fieldJsonValue));
                            } else {
                                patchBuilder.add("/" + entry.getKey(), jsonValueList.get(0));
                            }
                        }
                    }
                }
            }

            if (arrayObjectMap.get(typeName) != null) {
                for (Map.Entry<String, String> entry : arrayObjectMap.get(typeName)) {
                    List<JsonValue> jsonValueList = documentContext.read(entry.getValue());
                    if (jsonValueList.size() > 0) {
                        merged = true;
                        JsonValue fieldJsonValue = data.get(entry.getKey());
                        if (fieldJsonValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                            patchBuilder.add("/" + entry.getKey(), jsonValueList.stream().collect(JsonCollectors.toJsonArray()));
                        } else {
                            int newIndex = fieldJsonValue.asJsonArray().size();
                            for (JsonValue jsonValue : jsonValueList) {
                                Optional<JsonValue> original = fieldJsonValue.asJsonArray().stream()
                                        .filter(item -> item.asJsonObject().getString(idFieldName.get()).equals(jsonValue.asJsonObject().getString(idFieldName.get())))
                                        .findFirst();
                                if (original.isPresent()) {
                                    JsonMergePatch mergePatch = jsonProvider.createMergePatch(jsonValue);
                                    patchBuilder.replace("/" + entry.getKey() + "/" + fieldJsonValue.asJsonArray().indexOf(original.get()), mergePatch.apply(original.get()));
                                } else {
                                    patchBuilder.add("/" + entry.getKey() + "/" + newIndex++, jsonValue);
                                }
                            }
                        }
                    }
                }
            }

            if (connectionMap.get(typeName) != null) {
                for (Map.Entry<String, String> entry : connectionMap.get(typeName)) {
                    List<JsonValue> jsonValueList = documentContext.read(entry.getValue());
                    if (jsonValueList.size() > 0) {
                        merged = true;
                        JsonValue fieldJsonValue = data.get(entry.getKey());
                        if (fieldJsonValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                            patchBuilder.add(
                                    "/" + entry.getKey() + "/edges",
                                    jsonValueList.stream()
                                            .map(item ->
                                                    jsonProvider.createObjectBuilder()
                                                            .add("node", jsonProvider.createObjectBuilder(item.asJsonObject()))
                                                            .add("cursor",
                                                                    item.asJsonObject().get(
                                                                            manager.getFieldByDirective(typeName, CURSOR_DIRECTIVE_NAME)
                                                                                    .findFirst()
                                                                                    .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                                                                                    .orElse(idFieldName.get())
                                                                    )
                                                            )
                                                            .build()
                                            )
                                            .collect(JsonCollectors.toJsonArray())
                            );
                        } else {
                            int newIndex = fieldJsonValue.asJsonObject().getJsonArray("edges").size();
                            for (JsonValue jsonValue : jsonValueList) {
                                Optional<JsonValue> original = fieldJsonValue.asJsonObject().getJsonArray("edges").stream()
                                        .filter(item -> item.asJsonObject().getJsonObject("node").getString(idFieldName.get()).equals(jsonValue.asJsonObject().getString(idFieldName.get())))
                                        .findFirst();
                                if (original.isPresent()) {
                                    JsonMergePatch mergePatch = jsonProvider.createMergePatch(jsonValue);
                                    patchBuilder.replace(
                                            "/" + entry.getKey() + "/edges/" + fieldJsonValue.asJsonArray().indexOf(original.get()) + "/node",
                                            mergePatch.apply(original.get().asJsonObject().get("node"))
                                    );
                                } else {
                                    patchBuilder.add(
                                            "/" + entry.getKey() + "/edges/" + newIndex++,
                                            jsonProvider.createObjectBuilder()
                                                    .add("node", jsonProvider.createObjectBuilder(jsonValue.asJsonObject()))
                                                    .add("cursor",
                                                            jsonValue.asJsonObject().get(
                                                                    manager.getFieldByDirective(typeName, CURSOR_DIRECTIVE_NAME)
                                                                            .findFirst()
                                                                            .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                                                                            .orElse(idFieldName.get())
                                                            )
                                                    )
                                                    .build()
                                    );
                                }
                            }
                        }
                    }
                }
            }
            patchBuilder.build().apply(data);
        }
        return merged;
    }

    private void indexFilter(String typeName, GraphqlParser.SelectionSetContext selectionSetContext) {
        for (GraphqlParser.SelectionContext selectionContext : selectionSetContext.selection()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getField(typeName, selectionContext.field().name().getText())
                    .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                String fieldName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText();
                if (selectionContext.field().arguments() != null) {
                    Optional<Expression> expression = argumentsToFilter.argumentsToMultipleExpression(fieldDefinitionContext.argumentsDefinition(), selectionContext.field().arguments());
                    if (expression.isPresent()) {
                        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                            arrayObjectMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                            arrayObjectMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$[?" + expression.get() + "]"));
                        } else {
                            if (manager.isConnectionField(fieldDefinitionContext)) {
                                connectionMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                                connectionMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$[?" + expression.get() + "]"));
                            } else {
                                objectMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                                objectMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$[?" + expression.get() + "]"));
                            }
                        }
                    }
                } else {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                        arrayObjectMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                        arrayObjectMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$"));
                    } else {
                        if (manager.isConnectionField(fieldDefinitionContext)) {
                            connectionMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                            connectionMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$"));
                        } else {
                            objectMap.computeIfAbsent(fieldTypeName, k -> new ArrayList<>());
                            objectMap.get(fieldTypeName).add(new AbstractMap.SimpleEntry<>(fieldName, "$"));
                        }
                    }
                }
            }
        }
    }
}
