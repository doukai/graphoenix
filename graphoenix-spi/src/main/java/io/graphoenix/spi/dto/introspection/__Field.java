package io.graphoenix.spi.dto.introspection;

import java.util.List;

public class __Field {

    private String name;

    private String description;

    private List<__InputValue> args;

    private __Type type;

    private Boolean isDeprecated;

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

    public List<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(List<__InputValue> args) {
        this.args = args;
    }

    public __Type getType() {
        return type;
    }

    public void setType(__Type type) {
        this.type = type;
    }

    public Boolean getDeprecated() {
        return isDeprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        isDeprecated = deprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    @Override
    public String toString() {
        return "__Field{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", args=" + args +
                ", type=" + type +
                ", isDeprecated=" + isDeprecated +
                ", deprecationReason='" + deprecationReason + '\'' +
                '}';
    }
}
