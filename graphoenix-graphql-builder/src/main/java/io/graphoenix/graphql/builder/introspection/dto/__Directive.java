package io.graphoenix.graphql.builder.introspection.dto;

import java.util.List;

public class __Directive {

    private String name;

    private String description;

    private List<__DirectiveLocation> locations;

    private List<__InputValue> args;

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
}
