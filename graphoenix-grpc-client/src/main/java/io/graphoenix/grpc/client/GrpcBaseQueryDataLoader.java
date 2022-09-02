package io.graphoenix.grpc.client;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonCollectors;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static jakarta.json.JsonValue.NULL;

public class GrpcBaseQueryDataLoader {

    private Map<String, Map<String, Map<String, Set<String>>>> conditionMap;
    private Map<String, Map<String, Map<String, Set<Field>>>> fieldTree;

    public Mono<Operation> buildOperation(String packageName) {
        if (conditionMap == null || conditionMap.isEmpty() || conditionMap.get(packageName) == null || conditionMap.get(packageName).isEmpty()) {
            return Mono.empty();
        }
        return Mono.just(
                new Operation()
                        .setOperationType("query")
                        .setFields(
                                conditionMap.get(packageName).entrySet().stream()
                                        .flatMap(typeEntry ->
                                                typeEntry.getValue().entrySet().stream()
                                                        .filter(fieldEntry -> fieldEntry.getValue().size() > 0)
                                                        .map(fieldEntry ->
                                                                new Field()
                                                                        .setName(typeToLowerCamelName(typeEntry.getKey()).concat("List"))
                                                                        .setAlias(getQueryFieldAlias(typeEntry.getKey(), fieldEntry.getKey()))
                                                                        .addArgument(
                                                                                new Argument().setName(fieldEntry.getKey())
                                                                                        .setValueWithVariable(new ObjectValueWithVariable(Map.of("in", fieldEntry.getValue())))
                                                                        )
                                                                        .setFields(fieldTree.get(packageName).get(typeEntry.getKey()).get(fieldEntry.getKey()))
                                                        )
                                        )
                                        .collect(Collectors.toSet())
                        )
        );
    }

    public void addCondition(String packageName, String typeName, String fieldName, String key) {
        if (conditionMap == null) {
            conditionMap = new ConcurrentHashMap<>();
        }
        conditionMap.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        conditionMap.get(packageName).get(typeName).computeIfAbsent(fieldName, k -> new LinkedHashSet<>());
        conditionMap.get(packageName).get(typeName).get(fieldName).add(key);
    }

    public void mergeSelection(String packageName, String typeName, String fieldName, GraphqlParser.SelectionSetContext selectionSetContext) {
        if (fieldTree == null) {
            fieldTree = new ConcurrentHashMap<>();
        }
        fieldTree.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).computeIfAbsent(typeName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).get(typeName).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        mergeSelection(fieldTree.get(packageName).get(typeName).get(fieldName), selectionSetContext.selection().stream().map(Field::new).collect(Collectors.toSet()));
    }

    public void mergeSelection(Set<Field> originalSet, Set<Field> fieldSet) {
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

    public JsonValue jsonValueFilter(JsonValue jsonValue, GraphqlParser.SelectionSetContext selectionSetContext) {
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

    protected String getQueryFieldAlias(String typeName, String fieldName) {
        return typeName.concat("_").concat(fieldName);
    }

    public String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    protected void clear(String packageName) {
        this.conditionMap.get(packageName).clear();
        this.fieldTree.get(packageName).clear();
    }
}
