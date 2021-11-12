package io.graphoenix.spi.dto.introspection;

public class __EnumValue {

    private String name;

    private String description;

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
        return "__EnumValue{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isDeprecated=" + isDeprecated +
                ", deprecationReason='" + deprecationReason + '\'' +
                '}';
    }
}
