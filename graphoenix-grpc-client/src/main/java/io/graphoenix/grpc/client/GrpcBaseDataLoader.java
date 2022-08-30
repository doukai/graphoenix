package io.graphoenix.grpc.client;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

public class GrpcBaseDataLoader {

    private final IGraphQLDocumentManager manager;
    private Map<String, Map<String, Map<String, Set<String>>>> conditionMap;
    private Map<String, Map<String, Set<Field>>> fieldTree;

    public GrpcBaseDataLoader() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
    }

    public String valueWithVariableToString(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.objectValueWithVariable() != null) {
            return objectValueWithVariableToString(valueWithVariableContext.objectValueWithVariable());
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return "[".concat(
                    valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                            .map(this::valueWithVariableToString)
                            .collect(Collectors.joining(", "))
            ).concat("]");
        } else {
            return valueWithVariableContext.getText();
        }
    }

    public String objectValueWithVariableToString(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        return "{".concat(
                objectValueWithVariableContext.objectFieldWithVariable().stream()
                        .map(objectFieldWithVariableContext ->
                                objectFieldWithVariableContext.name().getText()
                                        .concat(": ")
                                        .concat(valueWithVariableToString(objectFieldWithVariableContext.valueWithVariable()))
                        )
                        .collect(Collectors.joining(" "))
        ).concat("}");
    }

    public List<String> objectValueWithVariableToStringList(GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {
        return arrayValueWithVariableContext.valueWithVariable().stream()
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .map(valueWithVariableContext -> objectValueWithVariableToString(valueWithVariableContext.objectValueWithVariable()))
                .collect(Collectors.toList());
    }

    public String getListArguments(Collection<String> argumentList) {
        return LIST_INPUT_NAME.concat(": [".concat(String.join(", ", argumentList)).concat("]"));
    }

    public Operation buildOperation(String packageName) {
        return new Operation()
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
                                                                .setFields(fieldTree.get(packageName).get(typeEntry.getKey()))
                                                )
                                )
                                .collect(Collectors.toSet())
                );
    }

    public void addCondition(String packageName, String typeName, String fieldName, String key) {
        if (conditionMap == null) {
            conditionMap = new HashMap<>();
        }
        conditionMap.computeIfAbsent(packageName, k -> new HashMap<>());
        conditionMap.get(packageName).computeIfAbsent(typeName, k -> new HashMap<>());
        conditionMap.get(packageName).get(typeName).computeIfAbsent(fieldName, k -> new LinkedHashSet<>());
        conditionMap.get(packageName).get(typeName).get(fieldName).add(key);
    }

    public void mergeSelection(String packageName, String typeName, GraphqlParser.SelectionSetContext selectionSetContext) {
        if (fieldTree == null) {
            fieldTree = new ConcurrentHashMap<>();
        }
        fieldTree.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());
        fieldTree.get(packageName).computeIfAbsent(typeName, k -> new LinkedHashSet<>());
        mergeSelection(packageName, fieldTree.get(packageName).get(typeName), selectionSetContext.selection().stream().map(Field::new).collect(Collectors.toSet()));
    }

    public void mergeSelection(String typeName, Set<Field> originalSet, Set<Field> fieldSet) {
        fieldSet.forEach(
                field -> {
                    if (originalSet.stream().map(Field::getName).noneMatch(name -> name.equals(field.getName()))) {
                        originalSet.add(field);
                    } else {
                        String fieldTypeName = manager.getField(typeName, field.getName())
                                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(typeName, field.getName())));
                        if (!manager.isScalar(fieldTypeName) && !manager.isEnum(fieldTypeName)) {
                            mergeSelection(
                                    fieldTypeName,
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
