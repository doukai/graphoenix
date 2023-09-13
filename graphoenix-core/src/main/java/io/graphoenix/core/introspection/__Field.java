package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_FIELD_NAME;

public class __Field {

    private String name;

    private String description;

    private Set<__InputValue> args;

    private __Type type;

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

    public Set<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(Set<__InputValue> args) {
        this.args = args;
    }

    public __Type getType() {
        return type;
    }

    public void setType(__Type type) {
        this.type = type;
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
        if (this.getArgs() != null) {
            objectValueWithVariable.put("args", this.getArgs().stream().map(__InputValue::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getType() != null) {
            objectValueWithVariable.put("typeName", this.getType().getName());
        }
        if (this.getIsDeprecated() != null) {
            objectValueWithVariable.put(DEPRECATED_FIELD_NAME, this.getIsDeprecated());
        }
        if (this.getDeprecationReason() != null) {
            objectValueWithVariable.put("deprecationReason", this.getDeprecationReason());
        }
        return objectValueWithVariable;
    }
}
