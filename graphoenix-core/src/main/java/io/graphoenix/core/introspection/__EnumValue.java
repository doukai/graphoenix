package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;

public class __EnumValue {

    private String name;

    private String description;

    private Boolean isDeprecated = false;

    private String deprecationReason;

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

    public Boolean getIsDeprecated() {
        return isDeprecated;
    }

    public void setIsDeprecated(Boolean deprecated) {
        isDeprecated = deprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    public ObjectValueWithVariable toObjectValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getName() != null) {
            objectValueWithVariable.put("name", this.getName());
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        if (this.getIsDeprecated() != null) {
            objectValueWithVariable.put("isDeprecated", this.getIsDeprecated());
        }
        if (this.getDeprecationReason() != null) {
            objectValueWithVariable.put("deprecationReason", this.getDeprecationReason());
        }
        return objectValueWithVariable;
    }
}
