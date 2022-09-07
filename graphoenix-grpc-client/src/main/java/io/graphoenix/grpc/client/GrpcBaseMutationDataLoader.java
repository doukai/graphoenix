package io.graphoenix.grpc.client;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.operation.ValueWithVariable;
import jakarta.json.JsonObject;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public class GrpcBaseMutationDataLoader {

    private Map<String, Map<String, Map<String, ObjectValueWithVariable>>> objectValueMap;
    private Map<String, Map<String, Set<String>>> selectionMap;

    public Mono<Operation> build(String packageName) {
        return Mono.fromSupplier(() -> buildOperation(packageName));
    }

    public Operation buildOperation(String packageName) {
        if (objectValueMap == null || objectValueMap.isEmpty() || objectValueMap.get(packageName) == null || objectValueMap.get(packageName).isEmpty()) {
            return null;
        }
        Operation operation = new Operation()
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
        this.clear(packageName);
        return operation;
    }

    public int addObjectValue(String packageName, String typeName, ObjectValueWithVariable objectValueWithVariable, String keyName) {
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
                return new ArrayList<>(typeValueMap.keySet()).indexOf(key);
            } else {
                typeValueMap.put(key, objectValueWithVariable);
                return typeValueMap.size() - 1;
            }
        } else {
            typeValueMap.put(UUID.randomUUID().toString(), objectValueWithVariable);
            return typeValueMap.size() - 1;
        }
    }

    public int addObjectValue(String packageName, String typeName, JsonObject jsonObject, String keyName) {
        return addObjectValue(packageName, typeName, new ObjectValueWithVariable(jsonObject), keyName);
    }

    public void addSelection(String packageName, String typeName, String selectionName) {
        if (selectionMap == null) {
            selectionMap = new ConcurrentHashMap<>();
        }
        selectionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        selectionMap.get(packageName).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        selectionMap.get(packageName).get(typeName).add(selectionName);
    }

    public String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    protected void clear(String packageName) {
        this.objectValueMap.get(packageName).clear();
        this.selectionMap.get(packageName).clear();
    }
}
