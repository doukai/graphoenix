package io.graphoenix.graphql.builder.introspection.vo;

public class __InputValue {

    private String name;

    private String description;

    private __Type type;

    private String defaultValue;

    private Boolean hasDescription;

    private Boolean hasDefaultValue;

    private Boolean isLast;

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

    public Boolean getHasDescription() {
        return hasDescription;
    }

    public void setHasDescription(Boolean hasDescription) {
        this.hasDescription = hasDescription;
    }

    public Boolean getHasDefaultValue() {
        return hasDefaultValue;
    }

    public void setHasDefaultValue(Boolean hasDefaultValue) {
        this.hasDefaultValue = hasDefaultValue;
    }

    public Boolean getLast() {
        return isLast;
    }

    public void setLast(Boolean last) {
        isLast = last;
    }
}
