package io.graphoenix.graphql.builder.introspection.dto;

import java.util.List;

public class __Field {

    private String id;

    private __Type type;

    private String name;

    private String description;

    private List<__FieldInputValue> args;

    private Boolean isDeprecated;

    private String deprecationReason;

    public String getId() {
        return id;
    }

    public __Field setId(String id) {
        this.id = id;
        return this;
    }

    public __Type getType() {
        return type;
    }

    public __Field setType(__Type type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public __Field setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public __Field setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<__FieldInputValue> getArgs() {
        return args;
    }

    public __Field setArgs(List<__FieldInputValue> args) {
        this.args = args;
        return this;
    }

    public Boolean getDeprecated() {
        return isDeprecated;
    }

    public __Field setDeprecated(Boolean deprecated) {
        isDeprecated = deprecated;
        return this;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public __Field setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
        return this;
    }
}
