package io.graphoenix.graphql.builder.introspection.vo;

public class __EnumValue {

    private String name;

    private String description;

    private Boolean isDeprecated;

    private String deprecationReason;

    private Boolean hasDescription;

    private Boolean hasDeprecationReason;

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

    public Boolean getHasDescription() {
        return hasDescription;
    }

    public void setHasDescription(Boolean hasDescription) {
        this.hasDescription = hasDescription;
    }

    public Boolean getHasDeprecationReason() {
        return hasDeprecationReason;
    }

    public void setHasDeprecationReason(Boolean hasDeprecationReason) {
        this.hasDeprecationReason = hasDeprecationReason;
    }

    public Boolean getLast() {
        return isLast;
    }

    public void setLast(Boolean last) {
        isLast = last;
    }
}
