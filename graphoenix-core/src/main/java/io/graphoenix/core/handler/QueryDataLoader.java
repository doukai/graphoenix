package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.*;
import io.graphoenix.spi.handler.FetchHandler;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_SUFFIX;
import static jakarta.json.JsonValue.NULL;

public abstract class QueryDataLoader {

    private final JsonProvider jsonProvider;
    private final Jsonb jsonb;
    private final Map<String, Map<String, Map<String, Map<String, Map<String, Map<JsonValue.ValueType, Set<Tuple2<String, GraphqlParser.SelectionSetContext>>>>>>>> conditionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Map<String, Set<Field>>>>> fieldTree = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<Tuple2<String, Field>>>> operationTypeFields = new ConcurrentHashMap<>();

    private final Map<String, Map<String, JsonValue>> resultMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> removeFiledMap = new ConcurrentHashMap<>();
    private final Map<String, List<Tuple3<String, JsonPointer, String>>> addFiledMap = new ConcurrentHashMap<>();

    public QueryDataLoader() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.jsonb = BeanContext.get(Jsonb.class);
    }

    public void register(String packageName, String protocol, String jsonPointer, GraphqlParser.SelectionContext selectionContext) {
        operationTypeFields.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        operationTypeFields.get(packageName).computeIfAbsent(protocol, k -> new LinkedHashSet<>());
        operationTypeFields.get(packageName).get(protocol).add(Tuple.of(jsonPointer, new Field(selectionContext.field())));
    }

    public void register(String packageName, String protocol, String typeName, String fieldName, JsonValue key, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        addSelection(packageName, protocol, typeName, fieldName, fieldName);
        mergeSelection(packageName, protocol, typeName, fieldName, selectionSetContext);
        addCondition(packageName, protocol, typeName, fieldName, getKeyValue(key), JsonValue.ValueType.OBJECT, jsonPointer, selectionSetContext);
    }

    public void registerArray(String packageName, String protocol, String typeName, String fieldName, JsonValue key, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        addSelection(packageName, protocol, typeName, fieldName, fieldName);
        mergeSelection(packageName, protocol, typeName, fieldName, selectionSetContext);
        addCondition(packageName, protocol, typeName, fieldName, getKeyValue(key), JsonValue.ValueType.ARRAY, jsonPointer, selectionSetContext);
    }

    public void registerRemoveFiled(String jsonPointer, String removeFiled) {
        removeFiledMap.computeIfAbsent(jsonPointer, k -> new ArrayList<>());
        removeFiledMap.get(jsonPointer).add(removeFiled);
    }

    public void registerAddFiled(String jsonPointer, String addField, JsonPointer addFieldJsonPointer, String element) {
        addFiledMap.computeIfAbsent(jsonPointer, k -> new ArrayList<>());
        addFiledMap.get(jsonPointer).add(Tuple.of(addField, addFieldJsonPointer, element));
    }

    public void registerReplaceFiled(String jsonPointer, String removeFiled, String addField, JsonPointer addFieldJsonPointer, String element) {
        registerRemoveFiled(jsonPointer, removeFiled);
        registerAddFiled(jsonPointer, addField, addFieldJsonPointer, element);
    }

    protected Mono<Operation> build(String packageName, String protocol) {
        return Mono.justOrEmpty(
                Optional.ofNullable(conditionMap.get(packageName))
                        .flatMap(protocolMap ->
                                Optional.ofNullable(protocolMap.get(protocol))
                                        .map(typeMap ->
                                                new Operation()
                                                        .setOperationType("query")
                                                        .addFields(
                                                                Stream.ofNullable(operationTypeFields.get(packageName))
                                                                        .flatMap(packageMap -> Stream.ofNullable(packageMap.get(protocol)))
                                                                        .flatMap(Collection::stream)
                                                                        .map(Tuple2::_2)
                                                                        .collect(Collectors.toSet())
                                                        )
                                                        .addFields(
                                                                typeMap.entrySet().stream()
                                                                        .flatMap(typeEntry ->
                                                                                typeEntry.getValue().entrySet().stream()
                                                                                        .filter(fieldEntry -> fieldEntry.getValue().keySet().size() > 0)
                                                                                        .map(fieldEntry ->
                                                                                                new Field()
                                                                                                        .setName(typeToLowerCamelName(typeEntry.getKey()) + LIST_SUFFIX)
                                                                                                        .setAlias(getQueryFieldAlias(typeEntry.getKey(), fieldEntry.getKey()))
                                                                                                        .addArgument(
                                                                                                                fieldEntry.getKey(),
                                                                                                                Map.of("in", fieldEntry.getValue().keySet())
                                                                                                        )
                                                                                                        .setFields(fieldTree.get(packageName).get(protocol).get(typeEntry.getKey()).get(fieldEntry.getKey()))
                                                                                        )
                                                                        )
                                                                        .collect(Collectors.toSet())
                                                        )
                                        )
                        )
        );
    }

    private void addCondition(String packageName, String protocol, String typeName, String fieldName, String key, JsonValue.ValueType valueType, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        conditionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(protocol).get(typeName).computeIfAbsent(fieldName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(protocol).get(typeName).get(fieldName).computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(protocol).get(typeName).get(fieldName).get(key).computeIfAbsent(valueType, k -> new LinkedHashSet<>());
        conditionMap.get(packageName).get(protocol).get(typeName).get(fieldName).get(key).get(valueType).add(Tuple.of(jsonPointer, selectionSetContext));
    }

    protected Mono<Void> fetch(String packageName, String protocol) {
        return build(packageName, protocol)
                .flatMap(operation -> BeanContext.get(FetchHandler.class, protocol).request(packageName, operation.toString()))
                .flatMap(response -> Mono.fromRunnable(() -> addResult(packageName, protocol, response)));
    }

    protected void addResult(String packageName, String protocol, String response) {
        JsonObject responseObject = jsonProvider.createReader(new StringReader(response)).readObject();
        if (responseObject.containsKey("errors")) {
            throw jsonb.fromJson(response, GraphQLErrors.class);
        }
        JsonObject jsonObject = responseObject.get("data").asJsonObject();
        resultMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        resultMap.get(packageName).put(protocol, jsonObject);
    }

    protected JsonValue dispatch(JsonObject jsonObject) {
        JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
        if (!operationTypeFields.isEmpty()) {
            operationTypeFields.forEach((packageName, protocolMap) ->
                    protocolMap.forEach((protocolName, fieldSet) ->
                            fieldSet.forEach(
                                    jsonPointer ->
                                            patchBuilder.add(
                                                    jsonPointer._1(),
                                                    jsonObject.get(Optional.ofNullable(jsonPointer._2().getAlias()).orElse(jsonPointer._2().getName()))
                                            )
                            )
                    )
            );
        }
        if (!conditionMap.isEmpty()) {
            conditionMap.forEach((packageName, packageMap) -> {
                        if (packageMap != null && !packageMap.isEmpty()) {
                            packageMap.forEach((protocolName, protocolMap) -> {
                                        if (protocolMap != null && !protocolMap.isEmpty()) {
                                            protocolMap.forEach((typeName, typeMap) -> {
                                                        if (typeMap != null && !typeMap.isEmpty()) {
                                                            typeMap.forEach((fieldName, fieldMap) -> {
                                                                        if (fieldMap != null && !fieldMap.isEmpty()) {
                                                                            fieldMap.forEach((key, valueTypeMap) -> {
                                                                                        if (valueTypeMap != null && !valueTypeMap.isEmpty()) {
                                                                                            valueTypeMap.forEach((valueType, jsonPointerList) -> {
                                                                                                        if (resultMap.containsKey(packageName) && resultMap.get(packageName).containsKey(protocolName)) {
                                                                                                            JsonObject data = resultMap.get(packageName).get(protocolName).asJsonObject();
                                                                                                            JsonValue fieldValue = data.get(getQueryFieldAlias(typeName, fieldName));
                                                                                                            if (fieldValue != null && fieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                                                                                                if (valueType.equals(JsonValue.ValueType.ARRAY)) {
                                                                                                                    Stream.ofNullable(jsonPointerList)
                                                                                                                            .flatMap(Collection::stream)
                                                                                                                            .forEach(jsonPointer ->
                                                                                                                                    patchBuilder.add(
                                                                                                                                            jsonPointer._1(),
                                                                                                                                            jsonValueFilter(
                                                                                                                                                    fieldValue.asJsonArray().stream()
                                                                                                                                                            .filter(item -> getKeyValue(item.asJsonObject().get(fieldName)).equals(key))
                                                                                                                                                            .collect(JsonCollectors.toJsonArray()),
                                                                                                                                                    jsonPointer._2()
                                                                                                                                            )
                                                                                                                                    )
                                                                                                                            );
                                                                                                                } else {
                                                                                                                    Stream.ofNullable(jsonPointerList)
                                                                                                                            .flatMap(Collection::stream)
                                                                                                                            .forEach(jsonPointer ->
                                                                                                                                    patchBuilder.add(
                                                                                                                                            jsonPointer._1(),
                                                                                                                                            jsonValueFilter(
                                                                                                                                                    fieldValue.asJsonArray().stream()
                                                                                                                                                            .filter(item -> getKeyValue(item.asJsonObject().get(fieldName)).equals(key))
                                                                                                                                                            .findFirst()
                                                                                                                                                            .orElse(NULL),
                                                                                                                                                    jsonPointer._2()
                                                                                                                                            )
                                                                                                                                    )
                                                                                                                            );
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                            );
                                                                                        }
                                                                                    }
                                                                            );
                                                                        }
                                                                    }
                                                            );
                                                        }
                                                    }
                                            );
                                        }
                                    }
                            );
                        }
                    }
            );
        }
        return patchBuilder.build().apply(jsonObject);
    }

    public JsonValue replaceAll(JsonObject jsonObject) {
        JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
        if (!addFiledMap.isEmpty()) {
            addFiledMap.forEach((key, value) ->
                    value.forEach((tuple3) -> {
                                JsonValue jsonValue = tuple3._2().getValue(jsonObject);
                                if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                    patchBuilder.add(key + "/" + tuple3._1(), tuple3._2().getValue(jsonObject).asJsonArray().stream().map(item -> item.asJsonObject().get(tuple3._3())).collect(JsonCollectors.toJsonArray()));
                                } else {
                                    patchBuilder.add(key + "/" + tuple3._1(), tuple3._2().getValue(jsonObject).asJsonObject().get(tuple3._3()));
                                }
                            }
                    )
            );
            addFiledMap.clear();
        }
        if (!removeFiledMap.isEmpty()) {
            removeFiledMap.forEach((key, value) ->
                    value.forEach((filedName) -> patchBuilder.remove(key + "/" + filedName))
            );
            removeFiledMap.clear();
        }
        return patchBuilder.build().apply(jsonObject);
    }

    private String getKeyValue(JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
            return ((JsonString) jsonValue).getString();
        } else {
            return jsonValue.toString();
        }
    }

    private void mergeSelection(String packageName, String protocol, String typeName, String fieldName, GraphqlParser.SelectionSetContext selectionSetContext) {
        mergeSelection(packageName, protocol, typeName, fieldName, selectionSetContext.selection().stream().map(Field::new).collect(Collectors.toSet()));
    }

    private void addSelection(String packageName, String protocol, String typeName, String fieldName, String selectionName) {
        mergeSelection(packageName, protocol, typeName, fieldName, Set.of(new Field().setName(selectionName)));
    }

    private void mergeSelection(String packageName, String protocol, String typeName, String fieldName, Set<Field> fieldSet) {
        fieldTree.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).get(protocol).get(typeName).computeIfAbsent(fieldName, k -> new LinkedHashSet<>());
        Field.mergeSelection(fieldTree.get(packageName).get(protocol).get(typeName).get(fieldName), fieldSet);
    }

    private JsonValue jsonValueFilter(JsonValue jsonValue, GraphqlParser.SelectionSetContext selectionSetContext) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            return selectionSetContext.selection().stream()
                    .map(selectionContext ->
                            new AbstractMap.SimpleEntry<>(
                                    selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText(),
                                    jsonValueFilter(jsonValue.asJsonObject().getOrDefault(selectionContext.field().name().getText(), NULL), selectionContext.field().selectionSet())
                            )
                    )
                    .collect(JsonCollectors.toJsonObject());
        } else if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            return jsonValue.asJsonArray().stream().map(item -> jsonValueFilter(item, selectionSetContext)).collect(JsonCollectors.toJsonArray());
        } else {
            return jsonValue;
        }
    }

    private String getQueryFieldAlias(String typeName, String fieldName) {
        return typeName + "_" + fieldName;
    }

    private String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX + typeToLowerCamelName(fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, ""));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public abstract Mono<JsonValue> load(JsonValue jsonValue);
}
