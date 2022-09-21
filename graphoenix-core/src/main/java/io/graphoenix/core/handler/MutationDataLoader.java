package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public abstract class MutationDataLoader {

    private final IGraphQLDocumentManager manager;
    private final JsonProvider jsonProvider;
    private Map<String, Map<String, Map<String, ObjectValueWithVariable>>> objectValueMap;
    private Map<String, Map<String, Set<String>>> selectionMap;
    private Map<String, Map<String, Map<Integer, List<Consumer<JsonObject>>>>> indexMap;
    private Map<String, JsonValue> resultMap;
    private Map<String, Set<String>> compensatingMap;

    public MutationDataLoader() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.resultMap = new ConcurrentHashMap<>();
    }

    public void register(String packageName, String typeName, String selectionName, String keyName, Consumer<JsonObject> callback, JsonValue jsonValue) {
        addSelection(packageName, typeName, keyName);
        if (selectionName != null) {
            addSelection(packageName, typeName, selectionName);
        }
        addObjectValue(packageName, typeName, keyName, callback, jsonValue.asJsonObject());
    }

    public void register(String packageName, String typeName, String selectionName, String keyName, Consumer<JsonObject> callback, ValueWithVariable valueWithVariable) {
        addSelection(packageName, typeName, keyName);
        if (selectionName != null) {
            addSelection(packageName, typeName, selectionName);
        }
        addObjectValue(packageName, typeName, keyName, callback, valueWithVariable.asObject());
    }

    public void registerArray(String packageName, String typeName, String selectionName, String keyName, Consumer<JsonObject> callback, JsonValue jsonValue) {
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            JsonArray jsonArray = jsonValue.asJsonArray();
            IntStream.range(0, jsonArray.size()).forEach(index -> register(packageName, typeName, selectionName, keyName, callback, jsonArray.get(index)));
        }
    }

    public void registerArray(String packageName, String typeName, String selectionName, String keyName, Consumer<JsonObject> callback, ValueWithVariable valueWithVariable) {
        if (valueWithVariable != null && valueWithVariable.isArray()) {
            ArrayValueWithVariable arrayValueWithVariable = valueWithVariable.asArray();
            IntStream.range(0, arrayValueWithVariable.size()).forEach(index -> register(packageName, typeName, selectionName, keyName, callback, arrayValueWithVariable.get(index)));
        }
    }

    public void register(String packageName, String typeName, String keyName, JsonValue jsonValue) {
        register(packageName, typeName, null, keyName, null, jsonValue);
    }

    public void register(String packageName, String typeName, String keyName, ValueWithVariable valueWithVariable) {
        register(packageName, typeName, null, keyName, null, valueWithVariable);
    }

    public void registerArray(String packageName, String typeName, String keyName, JsonValue jsonValue) {
        registerArray(packageName, typeName, null, keyName, null, jsonValue);
    }

    public void registerArray(String packageName, String typeName, String keyName, ValueWithVariable valueWithVariable) {
        registerArray(packageName, typeName, null, keyName, null, valueWithVariable);
    }

    public void registerCompensating(String typeName, String id) {
        if (compensatingMap == null) {
            compensatingMap = new ConcurrentHashMap<>();
        }
        compensatingMap.computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        compensatingMap.get(typeName).add(id);
    }

    protected Mono<Operation> build(String packageName) {
        return Mono.fromSupplier(() -> buildOperation(packageName));
    }

    private Operation buildOperation(String packageName) {
        if (objectValueMap == null || objectValueMap.isEmpty() || objectValueMap.get(packageName) == null || objectValueMap.get(packageName).isEmpty()) {
            return null;
        }
        return new Operation()
                .setOperationType("mutation")
                .setFields(
                        objectValueMap.get(packageName).entrySet().stream()
                                .filter(typeEntry -> typeEntry.getValue().size() > 0)
                                .map(typeEntry ->
                                        new Field()
                                                .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                .addArgument(
                                                        new Argument().setName(LIST_INPUT_NAME)
                                                                .setValueWithVariable(
                                                                        new ArrayValueWithVariable(typeEntry.getValue().values())
                                                                )
                                                )
                                                .setFields(selectionMap.get(packageName).get(typeEntry.getKey()).stream().map(Field::new).collect(Collectors.toSet()))
                                )
                                .collect(Collectors.toSet())
                );
    }

    private Operation buildCompensating() {
        if (compensatingMap == null || compensatingMap.isEmpty()) {
            return null;
        }
        return new Operation()
                .setOperationType("query")
                .setFields(
                        compensatingMap.entrySet().stream()
                                .filter(typeEntry -> typeEntry.getValue().size() > 0)
                                .map(typeEntry ->
                                        new Field()
                                                .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                .addArgument(
                                                        new Argument().setName(manager.getObjectTypeIDFieldName(typeEntry.getKey()).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST)))
                                                                .setValueWithVariable(
                                                                        new ArrayValueWithVariable(typeEntry.getValue())
                                                                )
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
                );
    }

    public void addObjectValue(String packageName, String typeName, String keyName, Consumer<JsonObject> callback, ObjectValueWithVariable objectValueWithVariable) {
        if (objectValueMap == null) {
            objectValueMap = new ConcurrentHashMap<>();
        }
        objectValueMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        objectValueMap.get(packageName).computeIfAbsent(typeName, k -> new LinkedHashMap<>());
        ValueWithVariable keyField = objectValueWithVariable.get(keyName);
        Map<String, ObjectValueWithVariable> typeValueMap = objectValueMap.get(packageName).get(typeName);
        if (keyField != null && keyField.isString()) {
            String key = keyField.asString().getValue();
            if (typeValueMap.containsKey(key)) {
                if (callback != null) {
                    addCallback(packageName, typeName, new ArrayList<>(typeValueMap.keySet()).indexOf(key), callback);
                }
            } else {
                typeValueMap.put(key, objectValueWithVariable);
                if (callback != null) {
                    addCallback(packageName, typeName, typeValueMap.size() - 1, callback);
                }
            }
        } else {
            typeValueMap.put(UUID.randomUUID().toString(), objectValueWithVariable);
            if (callback != null) {
                addCallback(packageName, typeName, typeValueMap.size() - 1, callback);
            }
        }
    }

    public void addObjectValue(String packageName, String typeName, String keyName, Consumer<JsonObject> callback, JsonObject jsonObject) {
        addObjectValue(packageName, typeName, keyName, callback, new ObjectValueWithVariable(jsonObject));
    }

    public void addSelection(String packageName, String typeName, String selectionName) {
        if (selectionMap == null) {
            selectionMap = new ConcurrentHashMap<>();
        }
        selectionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        selectionMap.get(packageName).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        selectionMap.get(packageName).get(typeName).add(selectionName);
    }

    public void addCallback(String packageName, String typeName, int index, Consumer<JsonObject> callback) {
        if (indexMap == null) {
            indexMap = new ConcurrentHashMap<>();
        }
        indexMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).get(typeName).computeIfAbsent(index, k -> new ArrayList<>());
        indexMap.get(packageName).get(typeName).get(index).add(callback);
    }

    protected void addResult(String packageName, String response) {
        JsonObject jsonObject = jsonProvider.createReader(new StringReader(response)).readObject().get("data").asJsonObject();
        if (resultMap == null) {
            resultMap = new ConcurrentHashMap<>();
        }
        resultMap.put(packageName, jsonObject);
    }

    protected void dispatch() {
        if (indexMap != null && !indexMap.isEmpty()) {
            indexMap.forEach((packageName, packageMap) -> {
                if (packageMap != null && !packageMap.isEmpty()) {
                    packageMap.forEach((typeName, typeMap) -> {
                        if (typeMap != null && !typeMap.isEmpty()) {
                            if (resultMap.containsKey(packageName)) {
                                JsonObject data = resultMap.get(packageName).asJsonObject();
                                JsonValue fieldValue = data.get(typeToLowerCamelName(typeName).concat("List"));
                                if (fieldValue != null && fieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                    JsonArray jsonArray = fieldValue.asJsonArray();
                                    IntStream.range(0, jsonArray.size()).forEach(index -> {
                                        List<Consumer<JsonObject>> callbackList = typeMap.get(index);
                                        if (callbackList != null && !callbackList.isEmpty()) {
                                            callbackList.forEach(callback -> callback.accept(jsonArray.get(index).asJsonObject()));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public abstract Mono<Void> load();
}
