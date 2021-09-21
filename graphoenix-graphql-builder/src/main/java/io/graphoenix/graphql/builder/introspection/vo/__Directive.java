package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Directive {

    private String name;

    private String description;

    private List<__DirectiveLocation> locations;

    private List<__InputValue> args;

    private Boolean hasDescription;

    private Boolean hasArgs;

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

    public List<__DirectiveLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<__DirectiveLocation> locations) {
        this.locations = locations;
    }

    public List<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(List<__InputValue> args) {
        this.args = args;
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

    public Boolean getLast() {
        return isLast;
    }

    public void setLast(Boolean last) {
        isLast = last;
    }
}
