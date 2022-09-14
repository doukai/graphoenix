package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.operation.ValueWithVariable;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public abstract class MutationDataLoader {

    private final JsonProvider jsonProvider;
    private Map<String, Map<String, Map<String, ObjectValueWithVariable>>> objectValueMap;
    private Map<String, Map<String, Set<String>>> selectionMap;
    private Map<String, Map<String, Map<Integer, Set<String>>>> indexMap;
    private Map<String, JsonValue> resultMap;

    public MutationDataLoader() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.resultMap = new ConcurrentHashMap<>();
    }

    public void register(String packageName, String typeName, String selectionName, String keyName, String path, JsonValue jsonValue) {
        addSelection(packageName, typeName, selectionName);
        addObjectValue(packageName, typeName, keyName, path, jsonValue.asJsonObject());
    }

    public void register(String packageName, String typeName, String selectionName, String keyName, String path, ValueWithVariable valueWithVariable) {
        addSelection(packageName, typeName, selectionName);
        addObjectValue(packageName, typeName, keyName, path, valueWithVariable.asObject());
    }

    public void registerArray(String packageName, String typeName, String selectionName, String keyName, String path, JsonValue jsonValue) {
        if (jsonValue != null && jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
            addSelection(packageName, typeName, selectionName);
            JsonArray jsonArray = jsonValue.asJsonArray();
            IntStream.range(0, jsonArray.size()).forEach(index -> register(packageName, typeName, selectionName, keyName, path + "/" + index, jsonArray.get(index)));
        }
    }

    public void registerArray(String packageName, String typeName, String selectionName, String keyName, String path, ValueWithVariable valueWithVariable) {
        if (valueWithVariable != null && valueWithVariable.isArray()) {
            addSelection(packageName, typeName, selectionName);
            ArrayValueWithVariable arrayValueWithVariable = valueWithVariable.asArray();
            IntStream.range(0, arrayValueWithVariable.size()).forEach(index -> register(packageName, typeName, selectionName, keyName, path + "/" + index, arrayValueWithVariable.get(index)));
        }
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

    public void addObjectValue(String packageName, String typeName, String keyName, String path, ObjectValueWithVariable objectValueWithVariable) {
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
                addPath(packageName, typeName, new ArrayList<>(typeValueMap.keySet()).indexOf(key), path);
            } else {
                typeValueMap.put(key, objectValueWithVariable);
                addPath(packageName, typeName, typeValueMap.size() - 1, path);
            }
        } else {
            typeValueMap.put(UUID.randomUUID().toString(), objectValueWithVariable);
            addPath(packageName, typeName, typeValueMap.size() - 1, path);
        }
    }

    public void addObjectValue(String packageName, String typeName, String keyName, String path, JsonObject jsonObject) {
        addObjectValue(packageName, typeName, keyName, path, new ObjectValueWithVariable(jsonObject));
    }

    public void addSelection(String packageName, String typeName, String selectionName) {
        if (selectionMap == null) {
            selectionMap = new ConcurrentHashMap<>();
        }
        selectionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        selectionMap.get(packageName).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        selectionMap.get(packageName).get(typeName).add(selectionName);
    }

    public void addPath(String packageName, String typeName, int index, String path) {
        if (indexMap == null) {
            indexMap = new ConcurrentHashMap<>();
        }
        indexMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        indexMap.get(packageName).get(typeName).computeIfAbsent(index, k -> new LinkedHashSet<>());
        indexMap.get(packageName).get(typeName).get(index).add(path);
    }

    protected void addResult(String packageName, String response) {
        JsonObject jsonObject = jsonProvider.createReader(new StringReader(response)).readObject().get("data").asJsonObject();
        if (resultMap == null) {
            resultMap = new ConcurrentHashMap<>();
        }
        resultMap.put(packageName, jsonObject);
    }

    protected JsonValue dispatch(JsonObject jsonObject) {
        JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
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
                                        Set<String> pathSet = typeMap.get(index);
                                        if (pathSet != null && !pathSet.isEmpty()) {
                                            pathSet.forEach(path -> patchBuilder.add(path, jsonArray.get(index)));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            });
        }
        return patchBuilder.build().apply(jsonObject);
    }

    private String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public abstract Mono<JsonValue> load(JsonValue jsonValue);
}
