package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Field {

    private String name;

    private String description;

    private List<__InputValue> args;

    private __Type type;

    private Boolean isDeprecated;

    private String deprecationReason;

    private Boolean hasDescription;

    private Boolean hasArgs;

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

    public Boolean getHasDescription() {
        return hasDescription;
    }

    public void setHasDescription(Boolean hasDescription) {
        this.hasDescription = hasDescription;
    }

    public Boolean getHasArgs() {
        return hasArgs;
    }

    public void setHasArgs(Boolean hasArgs) {
        this.hasArgs = hasArgs;
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
