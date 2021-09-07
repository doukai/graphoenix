package io.graphoenix.graphql.builder.introspection.dto;

import java.util.List;

public class __Type {

    private String id;

    private __Schema schema;

    private __TypeKind kind;

    private String name;

    private String description;

    private List<__Field> fields;

    private List<__Type> interfaces;

    private List<__Type> possibleTypes;

    private List<__EnumValue> enumValues;

    private List<__InputValue> inputFields;

    private __Type ofType;

    public String getId() {
        return id;
    }

    public __Type setId(String id) {
        this.id = id;
        return this;
    }

    public __Schema getSchema() {
        return schema;
    }

    public __Type setSchema(__Schema schema) {
        this.schema = schema;
        return this;
    }

    public __TypeKind getKind() {
        return kind;
    }

    public __Type setKind(__TypeKind kind) {
        this.kind = kind;
        return this;
    }

    public String getName() {
        return name;
    }

    public __Type setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public __Type setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<__Field> getFields() {
        return fields;
    }

    public __Type setFields(List<__Field> fields) {
        this.fields = fields;
        return this;
    }

    public List<__Type> getInterfaces() {
        return interfaces;
    }

    public __Type setInterfaces(List<__Type> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    public List<__Type> getPossibleTypes() {
        return possibleTypes;
    }

    public __Type setPossibleTypes(List<__Type> possibleTypes) {
        this.possibleTypes = possibleTypes;
        return this;
    }

    public List<__EnumValue> getEnumValues() {
        return enumValues;
    }

    public __Type setEnumValues(List<__EnumValue> enumValues) {
        this.enumValues = enumValues;
        return this;
    }

    public List<__InputValue> getInputFields() {
        return inputFields;
    }

    public __Type setInputFields(List<__InputValue> inputFields) {
        this.inputFields = inputFields;
        return this;
    }

    public __Type getOfType() {
        return ofType;
    }

    public __Type setOfType(__Type ofType) {
        this.ofType = ofType;
        return this;
    }
}
