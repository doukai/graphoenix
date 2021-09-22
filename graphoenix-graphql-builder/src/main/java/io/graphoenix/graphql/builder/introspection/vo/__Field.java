package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Field {

    private String name;

    private String description;

    private List<__InputValue> args;

    private __Type type;

    private Boolean isDeprecated;

    private String deprecationReason;

    private boolean hasDescription;

    private boolean hasArgs;

    private boolean hasDeprecationReason;

    private boolean last;

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

    public List<__InputValue> getArgs() {
        return args;
    }

    public __Field setArgs(List<__InputValue> args) {
        this.args = args;
        return this;
    }

    public __Type getType() {
        return type;
    }

    public __Field setType(__Type type) {
        this.type = type;
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

    public boolean isHasDescription() {
        return hasDescription;
    }

    public __Field setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
        return this;
    }

    public boolean isHasArgs() {
        return hasArgs;
    }

    public __Field setHasArgs(boolean hasArgs) {
        this.hasArgs = hasArgs;
        return this;
    }

    public boolean isHasDeprecationReason() {
        return hasDeprecationReason;
    }

    public __Field setHasDeprecationReason(boolean hasDeprecationReason) {
        this.hasDeprecationReason = hasDeprecationReason;
        return this;
    }

    public boolean isLast() {
        return last;
    }

    public __Field setLast(boolean last) {
        this.last = last;
        return this;
    }
}
