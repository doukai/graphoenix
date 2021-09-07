package io.graphoenix.graphql.builder.introspection.dto;

public class __EnumValue {

    private String id;

    private __Type type;

    private String name;

    private String description;

    private Boolean isDeprecated;

    private String deprecationReason;

    public String getId() {
        return id;
    }

    public __EnumValue setId(String id) {
        this.id = id;
        return this;
    }

    public __Type getType() {
        return type;
    }

    public __EnumValue setType(__Type type) {
        this.type = type;
        return this;
    }

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
}
