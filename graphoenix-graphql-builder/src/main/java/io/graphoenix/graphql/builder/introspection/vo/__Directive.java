package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Directive {

    private String name;

    private String description;

    private List<__DirectiveLocation> locations;

    private List<__InputValue> args;

    private boolean hasDescription;

    private boolean hasArgs;

    public String getName() {
        return name;
    }

    public __Directive setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public __Directive setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<__DirectiveLocation> getLocations() {
        return locations;
    }

    public __Directive setLocations(List<__DirectiveLocation> locations) {
        this.locations = locations;
        return this;
    }

    public List<__InputValue> getArgs() {
        return args;
    }

    public __Directive setArgs(List<__InputValue> args) {
        this.args = args;
        return this;
    }

    public boolean isHasDescription() {
        return hasDescription;
    }

    public __Directive setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
        return this;
    }

    public boolean isHasArgs() {
        return hasArgs;
    }

    public __Directive setHasArgs(boolean hasArgs) {
        this.hasArgs = hasArgs;
        return this;
    }
}
