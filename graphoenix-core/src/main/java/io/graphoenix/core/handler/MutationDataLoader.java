package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.EnumValue;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.constant.Hammurabi;
import io.graphoenix.spi.handler.FetchHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import jakarta.json.*;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_FIELD_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;

public abstract class MutationDataLoader {

    private final IGraphQLDocumentManager manager;
    private final GraphQLConfig graphQLConfig;
    private final JsonProvider jsonProvider;
    private final Jsonb jsonb;
    private final OperationHandler operationHandler;
    private final Map<String, Map<String, Map<String, Map<String, JsonObject>>>> objectValueMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Map<String, Set<JsonValue>>>>> removeConditionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Set<String>>>> selectionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Map<Integer, List<Tuple2<String, String>>>>>> indexMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, JsonValue>> resultMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> removeFiledMap = new ConcurrentHashMap<>();
    private final Map<String, List<Tuple2<String, JsonValue>>> addFiledMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> updateMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, JsonObject>> updateWhereMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> createMap = new ConcurrentHashMap<>();
    private String backUp;

    public MutationDataLoader() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.graphQLConfig = BeanContext.get(GraphQLConfig.class);
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.jsonb = BeanContext.get(Jsonb.class);
        this.operationHandler = BeanContext.get(OperationHandler.class);
    }

    public MutationDataLoader then() {
        this.objectValueMap.clear();
        this.selectionMap.clear();
        this.indexMap.clear();
        this.resultMap.clear();
        return this;
    }

    public void register(String packageName, String protocol, String typeName, String selectionName, String keyName, String jsonPointer, String element, JsonValue jsonValue, boolean removeOther) {
        addSelection(packageName, protocol, typeName, keyName);
        if (selectionName != null) {
            addSelection(packageName, protocol, typeName, selectionName);
        }
        addObjectValue(packageName, protocol, typeName, selectionName, keyName, jsonPointer, element, jsonValue.asJsonObject(), removeOther);
    }

    public void registerArray(String packageName, String protocol, String typeName, String selectionName, String keyName, String jsonPointer, String element, JsonValue jsonValue, boolean removeOther) {
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            JsonArray jsonArray = jsonValue.asJsonArray();
            IntStream.range(0, jsonArray.size()).forEach(index -> register(packageName, protocol, typeName, selectionName, keyName, jsonPointer + "/" + index, element, jsonArray.get(index), removeOther));
        }
    }

    public void register(String packageName, String protocol, String typeName, String keyName, JsonValue jsonValue) {
        register(packageName, protocol, typeName, null, keyName, null, null, jsonValue, false);
    }

    public void registerArray(String packageName, String protocol, String typeName, String keyName, JsonValue jsonValue) {
        registerArray(packageName, protocol, typeName, null, keyName, null, null, jsonValue, false);
    }

    public void registerRemoveFiled(String jsonPointer, String removeFiled) {
        removeFiledMap.computeIfAbsent(jsonPointer, k -> new ArrayList<>());
        removeFiledMap.get(jsonPointer).add(removeFiled);
    }

    public void registerAddFiled(String jsonPointer, String addField, JsonValue jsonValue) {
        addFiledMap.computeIfAbsent(jsonPointer, k -> new ArrayList<>());
        addFiledMap.get(jsonPointer).add(Tuple.of(addField, jsonValue));
    }

    public void registerReplaceFiled(String jsonPointer, String removeFiled, String addField, JsonValue jsonValue) {
        registerRemoveFiled(jsonPointer, removeFiled);
        registerAddFiled(jsonPointer, addField, jsonValue);
    }

    public void registerUpdate(String typeName, JsonValue jsonValue) {
        updateMap.computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
            updateMap.get(typeName).add(((JsonString) jsonValue).getString());
        }
    }

    public void registerWhere(String typeName, JsonValue jsonValue) {
        updateWhereMap.computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            Map<String, JsonObject> typeMap = updateWhereMap.get(typeName);
            typeMap.put(typeName + (typeMap.size() - 1), jsonValue.asJsonObject());
        }
    }

    public void registerCreate(String typeName, JsonValue jsonValue) {
        createMap.computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
            String id = ((JsonString) jsonValue).getString();
            if (!updateMap.get(typeName).contains(id)) {
                createMap.get(typeName).add(id);
            }
        }
    }

    protected Mono<Operation> build(String packageName, String protocol) {
        return Mono.justOrEmpty(
                Optional.ofNullable(objectValueMap.get(packageName))
                        .flatMap(protocolMap ->
                                Optional.ofNullable(protocolMap.get(protocol))
                                        .map(typeMap ->
                                                new Operation()
                                                        .setOperationType("mutation")
                                                        .setFields(
                                                                typeMap.entrySet().stream()
                                                                        .filter(typeEntry -> typeEntry.getValue().size() > 0)
                                                                        .map(typeEntry ->
                                                                                new Field()
                                                                                        .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                                        .addArgument(
                                                                                                LIST_INPUT_NAME,
                                                                                                typeEntry.getValue().values()
                                                                                        )
                                                                                        .setFields(selectionMap.get(packageName).get(protocol).get(typeEntry.getKey()).stream().map(Field::new).collect(Collectors.toSet()))
                                                                        )
                                                                        .collect(Collectors.toSet())
                                                        )
                                                        .addFields(buildRemoveMutationFields(packageName, protocol))
                                        )
                        )
        );
    }

    List<Field> buildRemoveMutationFields(String packageName, String protocol) {
        return Stream.ofNullable(removeConditionMap.get(packageName))
                .flatMap(protocolMap ->
                        Stream.ofNullable(protocolMap.get(protocol))
                                .flatMap(typeMap ->
                                        typeMap.entrySet().stream()
                                                .flatMap(typeEntry ->
                                                        typeEntry.getValue().entrySet().stream()
                                                                .map(selectionEntry ->
                                                                        new Field()
                                                                                .setName(typeToLowerCamelName(typeEntry.getKey()))
                                                                                .setAlias(typeToLowerCamelName(typeEntry.getKey()).concat("_").concat(selectionEntry.getKey()))
                                                                                .addArgument(
                                                                                        DEPRECATED_FIELD_NAME,
                                                                                        true
                                                                                )
                                                                                .addArgument(
                                                                                        WHERE_INPUT_NAME,
                                                                                        Map.of(
                                                                                                selectionEntry.getKey(),
                                                                                                selectionEntry.getValue().isEmpty() ?
                                                                                                        Map.of(
                                                                                                                "opr", new EnumValue("NNIL")
                                                                                                        ) :
                                                                                                        Map.of(
                                                                                                                "opr", new EnumValue("NIN"),
                                                                                                                "in", selectionEntry.getValue()
                                                                                                        )
                                                                                        )
                                                                                )
                                                                                .addField(new Field(typeEntry.getKey()))
                                                                )
                                                )
                                )
                )
                .collect(Collectors.toList());
    }

    public Mono<Void> backup() {
        return Mono.justOrEmpty(graphQLConfig.getBackup())
                .filter(backUp -> backUp)
                .flatMap(v -> buildBackupQuery())
                .flatMap(operation -> operationHandler.query(DOCUMENT_UTIL.graphqlToOperation(operation.toString())))
                .doOnNext(jsonString -> backUp = jsonString)
                .then();
    }

    protected Mono<Operation> buildBackupQuery() {
        return Mono.justOrEmpty(updateMap)
                .filter(updateMap -> !updateMap.isEmpty())
                .map(updateMap ->
                        new Operation()
                                .setOperationType("query")
                                .setFields(
                                        updateMap.entrySet().stream()
                                                .filter(typeEntry -> typeEntry.getValue().size() > 0)
                                                .map(typeEntry ->
                                                        new Field()
                                                                .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                .addArgument(
                                                                        manager.getObjectTypeIDFieldName(typeEntry.getKey()).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST)),
                                                                        new ObjectValueWithVariable(Map.of("in", typeEntry.getValue()))
                                                                )
                                                                .setFields(
                                                                        manager.getFields(typeEntry.getKey())
                                                                                .filter(fieldDefinitionContext -> !manager.isObject(typeEntry.getKey()))
                                                                                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                                                                                .map(Field::new)
                                                                                .collect(Collectors.toSet())
                                                                )
                                                )
                                                .collect(Collectors.toSet())
                                )
                                .addFields(
                                        updateWhereMap.entrySet().stream()
                                                .flatMap(typeEntry ->
                                                        typeEntry.getValue().entrySet().stream()
                                                                .map(whereEntry ->
                                                                        new Field()
                                                                                .setAlias(whereEntry.getKey())
                                                                                .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                                .addArgument(
                                                                                        WHERE_INPUT_NAME,
                                                                                        whereEntry.getValue()
                                                                                )
                                                                                .setFields(
                                                                                        manager.getFields(typeEntry.getKey())
                                                                                                .filter(fieldDefinitionContext -> !manager.isObject(typeEntry.getKey()))
                                                                                                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                                                                                                .map(Field::new)
                                                                                                .collect(Collectors.toSet())
                                                                                )
                                                                )
                                                )
                                                .collect(Collectors.toList())
                                )
                );
    }

    public Mono<Void> compensating(Throwable throwable) {
        return Mono.justOrEmpty(graphQLConfig.getCompensating())
                .filter(compensating -> compensating)
                .flatMap(v -> buildCompensatingMutation())
                .flatMap(operation -> operationHandler.mutation(DOCUMENT_UTIL.graphqlToOperation(operation.toString())))
                .then()
                .switchIfEmpty(Mono.error(throwable));
    }

    private Mono<Operation> buildCompensatingMutation() {
        return Mono.justOrEmpty(backUp)
                .map(jsonString -> jsonProvider.createReader(new StringReader(jsonString)).read())
                .filter(response -> !response.getValueType().equals(JsonValue.ValueType.OBJECT) || !response.asJsonObject().containsKey("data"))
                .map(response -> response.asJsonObject().getJsonObject("data"))
                .map(data ->
                        new Operation()
                                .setOperationType("mutation")
                                .setFields(
                                        Stream.concat(updateMap.keySet().stream(), createMap.keySet().stream())
                                                .collect(Collectors.toSet())
                                                .stream()
                                                .map(typeName ->
                                                        new Field()
                                                                .setName(typeToLowerCamelName(typeName).concat("List"))
                                                                .addArgument(
                                                                        LIST_INPUT_NAME,
                                                                        Stream.concat(
                                                                                getUpdateJsonArray(typeName, data),
                                                                                getRemoveJsonArray(typeName)
                                                                        ).collect(JsonCollectors.toJsonArray())
                                                                )
                                                                .addField(new Field(manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST))))
                                                )
                                                .collect(Collectors.toSet())
                                )
                );
    }

    private Stream<JsonValue> getUpdateJsonArray(String typeName, JsonObject data) {
        String idFieldName = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST));
        String selectionName = typeToLowerCamelName(typeName).concat("List");
        return io.vavr.collection.Stream.ofAll(
                Stream.concat(
                        Stream.of(data)
                                .filter(jsonObject ->
                                        data.containsKey(selectionName) &&
                                                data.get(selectionName).getValueType().equals(JsonValue.ValueType.ARRAY) &&
                                                data.get(selectionName).asJsonArray().size() > 0)
                                .flatMap(jsonObject -> jsonObject.get(selectionName).asJsonArray().stream()),
                        getUpdateWhereJsonArray(typeName, data)
                ).collect(Collectors.toList())
        )
                .distinctBy(jsonValue -> jsonValue.asJsonObject().get(idFieldName).toString())
                .toJavaStream();
    }


    private Stream<JsonValue> getUpdateWhereJsonArray(String typeName, JsonObject data) {
        return Stream.ofNullable(updateWhereMap.get(typeName))
                .flatMap(typeMap -> typeMap.keySet().stream())
                .filter(selectionName ->
                        data.containsKey(selectionName) &&
                                data.get(selectionName).getValueType().equals(JsonValue.ValueType.ARRAY) &&
                                data.get(selectionName).asJsonArray().size() > 0
                )
                .flatMap(selectionName -> data.get(selectionName).asJsonArray().stream());
    }

    private Stream<JsonValue> getRemoveJsonArray(String typeName) {
        String idFieldName = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST));
        return createMap.get(typeName).stream()
                .map(id -> {
                            JsonObjectBuilder jsonObjectBuilder = jsonProvider.createObjectBuilder();
                            jsonObjectBuilder.add(idFieldName, id);
                            jsonObjectBuilder.add(Hammurabi.DEPRECATED_FIELD_NAME, true);
                            return jsonObjectBuilder.build();
                        }
                );
    }

    public void addObjectValue(String packageName, String protocol, String typeName, String selectionName, String keyName, String jsonPointer, String element, JsonObject objectValueWithVariable, boolean removeOther) {
        objectValueMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        objectValueMap.get(packageName).computeIfAbsent(protocol, k -> new LinkedHashMap<>());
        objectValueMap.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new LinkedHashMap<>());
        JsonValue keyField = objectValueWithVariable.get(keyName);
        Map<String, JsonObject> typeValueMap = objectValueMap.get(packageName).get(protocol).get(typeName);
        String mergePath = element != null ? jsonPointer + "/" + element : jsonPointer;
        if (keyField != null && keyField.getValueType().equals(JsonValue.ValueType.STRING)) {
            String key = ((JsonString) keyField).getString();
            if (typeValueMap.containsKey(key)) {
                if (jsonPointer != null) {
                    addJsonPointer(packageName, protocol, typeName, new ArrayList<>(typeValueMap.keySet()).indexOf(key), mergePath, selectionName);
                }
            } else {
                typeValueMap.put(key, objectValueWithVariable);
                if (jsonPointer != null) {
                    addJsonPointer(packageName, protocol, typeName, typeValueMap.size() - 1, mergePath, selectionName);
                }
                if (removeOther) {
                    addRemoveCondition(packageName, protocol, typeName, element, selectionName, objectValueWithVariable);
                }
            }
        } else {
            typeValueMap.put(UUID.randomUUID().toString(), objectValueWithVariable);
            if (jsonPointer != null) {
                addJsonPointer(packageName, protocol, typeName, typeValueMap.size() - 1, mergePath, selectionName);
            }
        }
    }

    public void addRemoveCondition(String packageName, String protocol, String typeName, String element, String selectionName, JsonObject objectValueWithVariable) {
        removeConditionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        removeConditionMap.get(packageName).computeIfAbsent(protocol, k -> new LinkedHashMap<>());
        removeConditionMap.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new LinkedHashMap<>());
        removeConditionMap.get(packageName).get(protocol).get(typeName).computeIfAbsent(selectionName, k -> new LinkedHashSet<>());
        JsonValue elementField = objectValueWithVariable.get(element);
        if (elementField != null) {
            Set<JsonValue> conditionSet = removeConditionMap.get(packageName).get(protocol).get(typeName).get(selectionName);
            conditionSet.add(elementField);
        }
    }

    public void addSelection(String packageName, String protocol, String typeName, String selectionName) {
        selectionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        selectionMap.get(packageName).computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        selectionMap.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        selectionMap.get(packageName).get(protocol).get(typeName).add(selectionName);
    }

    public void addJsonPointer(String packageName, String protocol, String typeName, int index, String jsonPointer, String selectionName) {
        indexMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).get(protocol).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).get(protocol).get(typeName).computeIfAbsent(index, k -> new ArrayList<>());
        indexMap.get(packageName).get(protocol).get(typeName).get(index).add(Tuple.of(jsonPointer, selectionName));
    }

    protected Mono<Void> fetch(String packageName, String protocol) {
        return build(packageName, protocol)
                .flatMap(operation -> BeanContext.get(FetchHandler.class, protocol).operation(packageName, operation.toString()))
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
        if (!indexMap.isEmpty()) {
            indexMap.forEach((packageName, packageMap) -> {
                        if (packageMap != null && !packageMap.isEmpty()) {
                            packageMap.forEach((protocolName, protocolMap) -> {
                                        if (protocolMap != null && !protocolMap.isEmpty()) {
                                            protocolMap.forEach((typeName, typeMap) -> {
                                                        if (typeMap != null && !typeMap.isEmpty()) {
                                                            if (resultMap.containsKey(packageName) && resultMap.get(packageName).containsKey(protocolName)) {
                                                                JsonObject data = resultMap.get(packageName).get(protocolName).asJsonObject();
                                                                JsonValue fieldValue = data.get(typeToLowerCamelName(typeName).concat("List"));
                                                                if (fieldValue != null && fieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                                                    JsonArray jsonArray = fieldValue.asJsonArray();
                                                                    IntStream.range(0, jsonArray.size())
                                                                            .forEach(index -> {
                                                                                        List<Tuple2<String, String>> jsonPointerList = typeMap.get(index);
                                                                                        Stream.ofNullable(jsonPointerList)
                                                                                                .filter(list -> !list.isEmpty())
                                                                                                .flatMap(Collection::stream)
                                                                                                .forEach(jsonPointer ->
                                                                                                        patchBuilder.add(
                                                                                                                jsonPointer._1(),
                                                                                                                jsonArray.get(index).asJsonObject().get(jsonPointer._2())
                                                                                                        )
                                                                                                );
                                                                                    }
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
        return patchBuilder.build().apply(jsonObject);
    }

    public JsonValue replaceAll(JsonObject jsonObject) {
        JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
        if (!addFiledMap.isEmpty()) {
            addFiledMap.forEach((key, value) ->
                    value.forEach((tuple2) -> patchBuilder.add(key.concat("/").concat(tuple2._1()), tuple2._2()))
            );
            addFiledMap.clear();
        }
        if (!removeFiledMap.isEmpty()) {
            removeFiledMap.forEach((key, value) ->
                    value.forEach((filedName) -> patchBuilder.remove(key.concat("/").concat(filedName)))
            );
            removeFiledMap.clear();
        }
        return patchBuilder.build().apply(jsonObject);
    }

    private String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(typeToLowerCamelName(fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public JsonObject buildOperationArguments(Operation operation) {
        return operation.getFields().stream()
                .filter(field -> field.getArguments() != null && !field.getArguments().isEmpty())
                .map(field -> new AbstractMap.SimpleEntry<>(field.getAlias() != null ? field.getAlias() : field.getName(), (JsonValue) field.getArguments()))
                .collect(JsonCollectors.toJsonObject());
    }

    public Operation dispatchOperationArguments(JsonValue arguments, Operation operation) {
        operation.getFields().stream()
                .filter(field -> arguments.asJsonObject().containsKey(field.getAlias() != null ? field.getAlias() : field.getName()))
                .forEach(field -> field.setArguments(arguments.asJsonObject().getJsonObject(field.getAlias() != null ? field.getAlias() : field.getName())));
        return operation;
    }

    public abstract Mono<JsonValue> load(JsonValue jsonValue);

    public abstract Mono<Void> load();
}
