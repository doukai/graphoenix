package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class __Type {

    private __TypeKind kind;

    private String name;

    private String description;

    private Set<__Field> fields;

    private Set<__Type> interfaces;

    private Set<__Type> possibleTypes;

    private Set<__EnumValue> enumValues;

    private Set<__InputValue> inputFields;

    private __Type ofType;

    public __TypeKind getKind() {
        return kind;
    }

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

    public Set<__Field> getFields() {
        return fields;
    }

    public void setFields(Set<__Field> fields) {
        this.fields = fields;
    }

    public Set<__Type> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Set<__Type> interfaces) {
        this.interfaces = interfaces;
    }

    public Set<__Type> getPossibleTypes() {
        return possibleTypes;
    }

    public void setPossibleTypes(Set<__Type> possibleTypes) {
        this.possibleTypes = possibleTypes;
    }

    public Set<__EnumValue> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Set<__EnumValue> enumValues) {
        this.enumValues = enumValues;
    }

    public Set<__InputValue> getInputFields() {
        return inputFields;
    }

    public void setInputFields(Set<__InputValue> inputFields) {
        this.inputFields = inputFields;
    }

    public __Type getOfType() {
        return ofType;
    }

    public void setOfType(__Type ofType) {
        this.ofType = ofType;
    }

    public ObjectValueWithVariable toObjectValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getKind() != null) {
            objectValueWithVariable.put("kind", this.getKind());
        }
        if (this.getName() != null) {
            objectValueWithVariable.put("name", this.getName());
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        if (this.getFields() != null) {
            objectValueWithVariable.put("fields", this.getFields().stream().map(__Field::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getEnumValues() != null) {
            objectValueWithVariable.put("enumValues", this.getEnumValues().stream().map(__EnumValue::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getInputFields() != null) {
            objectValueWithVariable.put("inputFields", this.getInputFields().stream().map(__InputValue::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getOfType() != null) {
            objectValueWithVariable.put("ofTypeName", this.getOfType().getName());
        }
        return objectValueWithVariable;
    }

    public Stream<ObjectValueWithVariable> getInterfacesObjectValues() {
        return Stream.ofNullable(this.getInterfaces())
                .flatMap(Collection::stream)
                .map(interfaceType -> {
                            ObjectValueWithVariable __typeInterfaces = new ObjectValueWithVariable();
                            __typeInterfaces.put("typeName", this.getName());
                            __typeInterfaces.put("interfaceName", interfaceType.getName());
                            return __typeInterfaces;
                        }
                );
    }

    public Stream<ObjectValueWithVariable> getPossibleTypesObjectValues() {
        return Stream.ofNullable(this.getPossibleTypes())
                .flatMap(Collection::stream)
                .map(possibleType -> {
                            ObjectValueWithVariable __typePossibleTypes = new ObjectValueWithVariable();
                            __typePossibleTypes.put("typeName", this.getName());
                            __typePossibleTypes.put("possibleTypeName", possibleType.getName());
                            return __typePossibleTypes;
                        }
                );
    }
}
