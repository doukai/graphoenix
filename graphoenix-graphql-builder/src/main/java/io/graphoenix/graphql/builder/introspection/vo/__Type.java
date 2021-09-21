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

    public __TypeKind getKind() {
        return kind;
    }

    private Boolean hasDescription;

    private Boolean hasFields;

    private Boolean hasInterfaces;

    private Boolean hasPossibleTypes;

    private Boolean hasEnumValues;

    private Boolean hasInputFields;

    private Boolean hasOfType;

    private Boolean isLast;

    public void setKind(__TypeKind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<__Field> getFields() {
        return fields;
    }

    public void setFields(List<__Field> fields) {
        this.fields = fields;
    }

    public List<__Type> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<__Type> interfaces) {
        this.interfaces = interfaces;
    }

    public List<__Type> getPossibleTypes() {
        return possibleTypes;
    }

    public void setPossibleTypes(List<__Type> possibleTypes) {
        this.possibleTypes = possibleTypes;
    }

    public List<__EnumValue> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<__EnumValue> enumValues) {
        this.enumValues = enumValues;
    }

    public List<__InputValue> getInputFields() {
        return inputFields;
    }

    public void setInputFields(List<__InputValue> inputFields) {
        this.inputFields = inputFields;
    }

    public __Type getOfType() {
        return ofType;
    }

    public void setOfType(__Type ofType) {
        this.ofType = ofType;
    }

    public Boolean getHasDescription() {
        return hasDescription;
    }

    public void setHasDescription(Boolean hasDescription) {
        this.hasDescription = hasDescription;
    }

    public Boolean getHasFields() {
        return hasFields;
    }

    public void setHasFields(Boolean hasFields) {
        this.hasFields = hasFields;
    }

    public Boolean getHasInterfaces() {
        return hasInterfaces;
    }

    public void setHasInterfaces(Boolean hasInterfaces) {
        this.hasInterfaces = hasInterfaces;
    }

    public Boolean getHasPossibleTypes() {
        return hasPossibleTypes;
    }

    public void setHasPossibleTypes(Boolean hasPossibleTypes) {
        this.hasPossibleTypes = hasPossibleTypes;
    }

    public Boolean getHasEnumValues() {
        return hasEnumValues;
    }

    public void setHasEnumValues(Boolean hasEnumValues) {
        this.hasEnumValues = hasEnumValues;
    }

    public Boolean getHasInputFields() {
        return hasInputFields;
    }

    public void setHasInputFields(Boolean hasInputFields) {
        this.hasInputFields = hasInputFields;
    }

    public Boolean getHasOfType() {
        return hasOfType;
    }

    public void setHasOfType(Boolean hasOfType) {
        this.hasOfType = hasOfType;
    }

    public Boolean getLast() {
        return isLast;
    }

    public void setLast(Boolean last) {
        isLast = last;
    }
}
