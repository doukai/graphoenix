package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static jakarta.json.JsonValue.NULL;

public abstract class QueryDataLoader {

    private final JsonProvider jsonProvider;
    private Map<String, Map<String, Map<String, Map<String, Map<JsonValue.ValueType, Set<Tuple2<String, GraphqlParser.SelectionSetContext>>>>>>> conditionMap;
    private Map<String, Map<String, Map<String, Set<Field>>>> fieldTree;
    private Map<String, JsonValue> resultMap;

    public QueryDataLoader() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.resultMap = new ConcurrentHashMap<>();
    }

    public void register(String packageName, String typeName, String fieldName, JsonValue key, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        addSelection(packageName, typeName, fieldName, fieldName);
        mergeSelection(packageName, typeName, fieldName, selectionSetContext);
        addCondition(packageName, typeName, fieldName, getKeyValue(key), JsonValue.ValueType.OBJECT, jsonPointer, selectionSetContext);
    }

    public void registerArray(String packageName, String typeName, String fieldName, JsonValue key, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        addSelection(packageName, typeName, fieldName, fieldName);
        mergeSelection(packageName, typeName, fieldName, selectionSetContext);
        addCondition(packageName, typeName, fieldName, getKeyValue(key), JsonValue.ValueType.ARRAY, jsonPointer, selectionSetContext);
    }

    protected Mono<Operation> build(String packageName) {
        return Mono.fromSupplier(() -> buildOperation(packageName));
    }

    private Operation buildOperation(String packageName) {
        if (conditionMap == null || conditionMap.isEmpty() || conditionMap.get(packageName) == null || conditionMap.get(packageName).isEmpty()) {
            return null;
        }
        return new Operation()
                .setOperationType("query")
                .setFields(
                        conditionMap.get(packageName).entrySet().stream()
                                .flatMap(typeEntry ->
                                        typeEntry.getValue().entrySet().stream()
                                                .filter(fieldEntry -> fieldEntry.getValue().keySet().size() > 0)
                                                .map(fieldEntry ->
                                                        new Field()
                                                                .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                .setAlias(getQueryFieldAlias(typeEntry.getKey(), fieldEntry.getKey()))
                                                                .addArgument(
                                                                        new Argument().setName(fieldEntry.getKey())
                                                                                .setValueWithVariable(new ObjectValueWithVariable(Map.of("in", fieldEntry.getValue().keySet())))
                                                                )
                                                                .setFields(fieldTree.get(packageName).get(typeEntry.getKey()).get(fieldEntry.getKey()))
                                                )
                                )
                                .collect(Collectors.toSet())
                );
    }

    private void addCondition(String packageName, String typeName, String fieldName, String key, JsonValue.ValueType valueType, String jsonPointer, GraphqlParser.SelectionSetContext selectionSetContext) {
        if (conditionMap == null) {
            conditionMap = new ConcurrentHashMap<>();
        }
        conditionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(typeName).computeIfAbsent(fieldName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(typeName).get(fieldName).computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(typeName).get(fieldName).get(key).computeIfAbsent(valueType, k -> new LinkedHashSet<>());
        conditionMap.get(packageName).get(typeName).get(fieldName).get(key).get(valueType).add(Tuple.of(jsonPointer, selectionSetContext));
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
        if (conditionMap != null && !conditionMap.isEmpty()) {
            conditionMap.forEach((packageName, packageMap) -> {
                if (packageMap != null && !packageMap.isEmpty()) {
                    packageMap.forEach((typeName, typeMap) -> {
                        if (typeMap != null && !typeMap.isEmpty()) {
                            typeMap.forEach((fieldName, fieldMap) -> {
                                if (fieldMap != null && !fieldMap.isEmpty()) {
                                    fieldMap.forEach((key, valueTypeMap) -> {
                                        if (valueTypeMap != null && !valueTypeMap.isEmpty()) {
                                            valueTypeMap.forEach((valueType, jsonPointerList) -> {
                                                if (resultMap.containsKey(packageName)) {
                                                    JsonObject data = resultMap.get(packageName).asJsonObject();
                                                    JsonValue fieldValue = data.get(getQueryFieldAlias(typeName, fieldName));
                                                    if (fieldValue != null && fieldValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                                        if (valueType.equals(JsonValue.ValueType.ARRAY)) {
                                                            if (jsonPointerList != null && !jsonPointerList.isEmpty()) {
                                                                jsonPointerList.forEach(jsonPointer ->
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
                                                            }
                                                        } else {
                                                            if (jsonPointerList != null && !jsonPointerList.isEmpty()) {
                                                                jsonPointerList.forEach(jsonPointer ->
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
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
        return patchBuilder.build().apply(jsonObject);
    }

    private String getKeyValue(JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
            String string = jsonValue.toString();
            return string.substring(1, string.length() - 1);
        } else {
            return jsonValue.toString();
        }
    }

    private void mergeSelection(String packageName, String typeName, String fieldName, GraphqlParser.SelectionSetContext selectionSetContext) {
        mergeSelection(packageName, typeName, fieldName, selectionSetContext.selection().stream().map(Field::new).collect(Collectors.toSet()));
    }

    private void addSelection(String packageName, String typeName, String fieldName, String selectionName) {
        mergeSelection(packageName, typeName, fieldName, Set.of(new Field().setName(selectionName)));
    }

    private void mergeSelection(String packageName, String typeName, String fieldName, Set<Field> fieldSet) {
        if (fieldTree == null) {
            fieldTree = new ConcurrentHashMap<>();
        }
        fieldTree.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).get(typeName).computeIfAbsent(fieldName, k -> new LinkedHashSet<>());
        mergeSelection(fieldTree.get(packageName).get(typeName).get(fieldName), fieldSet);
    }

    private void mergeSelection(Set<Field> originalSet, Set<Field> fieldSet) {
        fieldSet.forEach(
                field -> {
                    if (originalSet.stream().map(Field::getName).noneMatch(name -> name.equals(field.getName()))) {
                        originalSet.add(field);
                    } else {
                        if (field.getFields() != null && field.getFields().size() > 0) {
                            mergeSelection(
                                    originalSet.stream()
                                            .filter(original -> original.getName().equals(field.getName()))
                                            .findFirst()
                                            .orElseThrow(() -> new GraphQLErrors(SELECTION_NOT_EXIST.bind(field.getName())))
                                            .getFields(),
                                    field.getFields()
                            );
                        }
                    }
                }
        );
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
        return typeName.concat("_").concat(fieldName);
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
