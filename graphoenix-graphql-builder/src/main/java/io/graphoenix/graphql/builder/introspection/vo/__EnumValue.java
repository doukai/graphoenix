package io.graphoenix.graphql.builder.introspection.vo;

public class __EnumValue {

    private String name;

    private String description;

    private Boolean isDeprecated;

    private String deprecationReason;

    private boolean hasDescription;

    private boolean hasDeprecationReason;

    public String getName() {
        return name;
    }

    public __EnumValue setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public __EnumValue setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getDeprecated() {
        return isDeprecated;
    }

    public __EnumValue setDeprecated(Boolean deprecated) {
        isDeprecated = deprecated;
        return this;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public __EnumValue setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
        return this;
    }

    public boolean isHasDescription() {
        return hasDescription;
    }

    public __EnumValue setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
        return this;
    }

    public boolean isHasDeprecationReason() {
        return hasDeprecationReason;
    }

    public __EnumValue setHasDeprecationReason(boolean hasDeprecationReason) {
        this.hasDeprecationReason = hasDeprecationReason;
        return this;
    }
}
