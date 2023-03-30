package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;

public class __InputValue {

    private String name;

    private String description;

    private __Type type;

    private String defaultValue;

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

    public __Type getType() {
        return type;
    }

    public void setType(__Type type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ObjectValueWithVariable toObjectValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getName() != null) {
            objectValueWithVariable.put("name", this.getName());
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        if (this.getDefaultValue() != null) {
            objectValueWithVariable.put("defaultValue", this.getDefaultValue());
        }
        if (this.getType() != null) {
            objectValueWithVariable.put("typeName", this.getType().getName());
        }
        return objectValueWithVariable;
    }
}
