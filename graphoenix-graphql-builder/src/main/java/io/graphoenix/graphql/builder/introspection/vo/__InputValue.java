package io.graphoenix.graphql.builder.introspection.vo;

public class __InputValue {

    private String name;

    private String description;

    private __Type type;

    private String defaultValue;

    private boolean hasDescription;

    private boolean hasDefaultValue;

    private boolean last;

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

    public __Type getType() {
        return type;
    }

    public __InputValue setType(__Type type) {
        this.type = type;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public __InputValue setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isHasDescription() {
        return hasDescription;
    }

    public __InputValue setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
        return this;
    }

    public boolean isHasDefaultValue() {
        return hasDefaultValue;
    }

    public __InputValue setHasDefaultValue(boolean hasDefaultValue) {
        this.hasDefaultValue = hasDefaultValue;
        return this;
    }

    public boolean isLast() {
        return last;
    }

    public __InputValue setLast(boolean last) {
        this.last = last;
        return this;
    }
}
