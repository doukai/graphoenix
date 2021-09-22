package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Type {

    private __TypeKind kind;

    private String name;

    private String description;

    private List<__Field> fields;

    private List<__Type> interfaces;

    private List<__Type> possibleTypes;

    private List<__EnumValue> enumValues;

    private List<__InputValue> inputFields;

    private __Type ofType;

    private boolean hasName;

    private boolean hasDescription;

    private boolean hasFields;

    private boolean hasInterfaces;

    private boolean hasPossibleTypes;

    private boolean hasEnumValues;

    private boolean hasInputFields;

    private boolean hasOfType;

    private boolean last;

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

    public boolean isHasDescription() {
        return hasDescription;
    }

    public boolean isHasName() {
        return hasName;
    }

    public __Type setHasName(boolean hasName) {
        this.hasName = hasName;
        return this;
    }

    public __Type setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
        return this;
    }

    public boolean isHasFields() {
        return hasFields;
    }

    public __Type setHasFields(boolean hasFields) {
        this.hasFields = hasFields;
        return this;
    }

    public boolean isHasInterfaces() {
        return hasInterfaces;
    }

    public __Type setHasInterfaces(boolean hasInterfaces) {
        this.hasInterfaces = hasInterfaces;
        return this;
    }

    public boolean isHasPossibleTypes() {
        return hasPossibleTypes;
    }

    public __Type setHasPossibleTypes(boolean hasPossibleTypes) {
        this.hasPossibleTypes = hasPossibleTypes;
        return this;
    }

    public boolean isHasEnumValues() {
        return hasEnumValues;
    }

    public __Type setHasEnumValues(boolean hasEnumValues) {
        this.hasEnumValues = hasEnumValues;
        return this;
    }

    public boolean isHasInputFields() {
        return hasInputFields;
    }

    public __Type setHasInputFields(boolean hasInputFields) {
        this.hasInputFields = hasInputFields;
        return this;
    }

    public boolean isHasOfType() {
        return hasOfType;
    }

    public __Type setHasOfType(boolean hasOfType) {
        this.hasOfType = hasOfType;
        return this;
    }

    public boolean isLast() {
        return last;
    }

    public __Type setLast(boolean last) {
        this.last = last;
        return this;
    }
}
