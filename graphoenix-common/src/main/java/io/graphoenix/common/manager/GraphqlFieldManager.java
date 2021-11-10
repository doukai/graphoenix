package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphqlFieldManager;
import io.graphoenix.spi.dto.map.FieldMap;
import io.graphoenix.spi.dto.map.FieldMapWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlFieldManager implements IGraphqlFieldManager {

    private final Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> fieldDefinitionTree = new HashMap<>();

    private final Map<String, Map<String, FieldMap>> fieldMapTree = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        fieldDefinitionTree.put(objectTypeDefinitionContext.name().getText(),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));
        return fieldDefinitionTree;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        fieldDefinitionTree.put(interfaceTypeDefinitionContext.name().getText(),
                interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .collect(Collectors.toMap(fieldDefinitionContext -> fieldDefinitionContext.name().getText(), fieldDefinitionContext -> fieldDefinitionContext)));
        return fieldDefinitionTree;
    }

    @Override
    public Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitions(String objectTypeName) {
        return fieldDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue)
                .flatMap(stringFieldDefinitionContextMap -> stringFieldDefinitionContextMap.values().stream());
    }

    @Override
    public Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinition(String objectTypeName, String fieldName) {
        return fieldDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue).findFirst()
                .flatMap(fieldDefinitionMap -> fieldDefinitionMap.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(fieldName))
                        .map(Map.Entry::getValue).findFirst());
    }

    @Override
    public Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                          String fieldName,
                                                          GraphqlParser.FieldDefinitionContext from,
                                                          GraphqlParser.ObjectTypeDefinitionContext toType,
                                                          GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, toType, to));
        fieldMapTree.put(objectTypeName, fieldMap);
        return fieldMapTree;
    }

    @Override
    public Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                          String fieldName,
                                                          GraphqlParser.FieldDefinitionContext from,
                                                          GraphqlParser.ObjectTypeDefinitionContext withType,
                                                          GraphqlParser.FieldDefinitionContext withFrom,
                                                          GraphqlParser.FieldDefinitionContext withTo,
                                                          GraphqlParser.ObjectTypeDefinitionContext toType,
                                                          GraphqlParser.FieldDefinitionContext to) {
        Map<String, FieldMap> fieldMap = fieldMapTree.get(objectTypeName);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        fieldMap.put(fieldName, new FieldMap(from, new FieldMapWith(withType, withFrom, withTo), toType, to));
        fieldMapTree.put(objectTypeName, fieldMap);
        return fieldMapTree;
    }

    @Override
    public GraphqlParser.FieldDefinitionContext getFromFieldDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getFrom();
    }

    @Override
    public GraphqlParser.ObjectTypeDefinitionContext getToObjectTypeDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getToType();
    }

    @Override
    public GraphqlParser.FieldDefinitionContext getToFieldDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getTo();
    }

    @Override
    public boolean mapWithType(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).withType();
    }

    @Override
    public GraphqlParser.ObjectTypeDefinitionContext getWithObjectTypeDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getWith().getType();
    }

    @Override
    public GraphqlParser.FieldDefinitionContext getWithFromFieldDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getWith().getFrom();
    }

    @Override
    public GraphqlParser.FieldDefinitionContext getWithToFieldDefinition(String objectTypeName, String fieldName) {
        return this.fieldMapTree.get(objectTypeName).get(fieldName).getWith().getTo();
    }
}
