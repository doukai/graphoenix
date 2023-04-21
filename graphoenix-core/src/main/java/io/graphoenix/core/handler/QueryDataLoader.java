package io.graphoenix.core.handler;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.*;
import io.graphoenix.spi.handler.FetchHandler;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static jakarta.json.JsonValue.NULL;

public abstract class QueryDataLoader {

    private final JsonProvider jsonProvider;
    private final Map<String, Map<String, Map<String, Map<String, Map<String, Map<JsonValue.ValueType, Set<Tuple2<String, GraphqlParser.SelectionSetContext>>>>>>>> conditionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Map<String, Set<Field>>>>> fieldTree = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<Tuple2<String, Field>>>> operationTypeFields = new ConcurrentHashMap<>();

    private final Map<String, Map<String, JsonValue>> resultMap = new ConcurrentHashMap<>();

    public QueryDataLoader() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
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

    protected Mono<Operation> build(String packageName, String protocol) {
        return Mono.justOrEmpty(
                Optional.of(conditionMap)
                        .filter(map -> !map.isEmpty())
                        .flatMap(map -> Optional.ofNullable(map.get(packageName)))
                        .flatMap(protocolMap ->
                                Optional.of(protocolMap)
                                        .filter(map -> !map.isEmpty())
                                        .flatMap(map -> Optional.ofNullable(map.get(protocol)))
                                        .filter(map -> !map.isEmpty())
                                        .map(typeMap ->
                                                new Operation()
                                                        .setOperationType("query")
                                                        .addFields(
                                                                Stream.ofNullable(operationTypeFields.get(packageName))
                                                                        .flatMap(packageMap -> Stream.ofNullable(packageMap.get(protocol)))
                                                                        .flatMap(Collection::stream)
                                                                        .map(Tuple2::_2).collect(Collectors.toSet())
                                                        )
                                                        .addFields(
                                                                typeMap.entrySet().stream()
                                                                        .flatMap(typeEntry ->
                                                                                typeEntry.getValue().entrySet().stream()
                                                                                        .filter(fieldEntry -> fieldEntry.getValue().keySet().size() > 0)
                                                                                        .map(fieldEntry ->
                                                                                                new Field()
                                                                                                        .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                                                        .setAlias(getQueryFieldAlias(typeEntry.getKey(), fieldEntry.getKey()))
                                                                                                        .addArgument(
                                                                                                                fieldEntry.getKey(),
                                                                                                                new ObjectValueWithVariable().put("in", new ArrayValueWithVariable(fieldEntry.getValue().keySet()))
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
                .flatMap(operation -> BeanContext.get(FetchHandler.class, protocol).operation(packageName, operation.toString()))
                .flatMap(response -> Mono.fromRunnable(() -> addResult(packageName, protocol, response)));
    }

    protected void addResult(String packageName, String protocol, String response) {
        JsonObject jsonObject = jsonProvider.createReader(new StringReader(response)).readObject().get("data").asJsonObject();
        resultMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        resultMap.get(packageName).put(protocol, jsonObject);
    }

    protected JsonValue dispatch(JsonObject jsonObject) {
        JsonPatchBuilder patchBuilder = jsonProvider.createPatchBuilder();
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
        Optional.of(conditionMap)
                .filter(map -> !map.isEmpty()).stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach((packageName, packageMap) -> {
                    Optional.of(packageMap)
                            .filter(map -> !map.isEmpty()).stream()
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                            .forEach((protocolName, protocolMap) -> {
                                Optional.of(protocolMap)
                                        .filter(map -> !map.isEmpty()).stream()
                                        .flatMap(map -> map.entrySet().stream())
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                        .forEach((typeName, typeMap) -> {
                                            Optional.of(typeMap)
                                                    .filter(map -> !map.isEmpty()).stream()
                                                    .flatMap(map -> map.entrySet().stream())
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                                    .forEach((fieldName, fieldMap) -> {
                                                        Optional.of(fieldMap)
                                                                .filter(map -> !map.isEmpty()).stream()
                                                                .flatMap(map -> map.entrySet().stream())
                                                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                                                .forEach((key, valueTypeMap) -> {
                                                                    Optional.of(valueTypeMap)
                                                                            .filter(map -> !map.isEmpty()).stream()
                                                                            .flatMap(map -> map.entrySet().stream())
                                                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                                                            .forEach((valueType, jsonPointerList) -> {
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
                                                                            });
                                                                });
                                                    });
                                        });
                            });
                });
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
        mergeSelection(fieldTree.get(packageName).get(protocol).get(typeName).get(fieldName), fieldSet);
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
            return INTROSPECTION_PREFIX.concat(typeToLowerCamelName(fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public abstract Mono<JsonValue> load(JsonValue jsonValue);
}
