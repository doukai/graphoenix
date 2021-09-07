package io.graphoenix.graphql.builder.introspection.dto;

import java.util.List;

public class __Directive {

    private String id;

    private __Schema schema;

    private String name;

    private String description;

    private List<__DirectiveLocation> locations;

    private List<__DirectiveInputValue> args;

    public String getId() {
        return id;
    }

    public __Directive setId(String id) {
        this.id = id;
        return this;
    }

    public __Schema getSchema() {
        return schema;
    }

    public __Directive setSchema(__Schema schema) {
        this.schema = schema;
        return this;
    }

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

    public List<__DirectiveInputValue> getArgs() {
        return args;
    }

    public __Directive setArgs(List<__DirectiveInputValue> args) {
        this.args = args;
        return this;
    }
}
