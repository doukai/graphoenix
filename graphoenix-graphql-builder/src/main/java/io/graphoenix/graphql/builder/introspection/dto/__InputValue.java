package io.graphoenix.graphql.builder.introspection.dto;

public class __InputValue {

    private String id;

    private __Type type;

    private String name;

    private String description;

    private String defaultValue;

    public String getId() {
        return id;
    }

    public __InputValue setId(String id) {
        this.id = id;
        return this;
    }

    public __Type getType() {
        return type;
    }

    public __InputValue setType(__Type type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public __InputValue setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public __InputValue setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public __InputValue setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
