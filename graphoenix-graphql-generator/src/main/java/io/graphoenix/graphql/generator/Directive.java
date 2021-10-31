package io.graphoenix.graphql.generator;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Directive {

    private final STGroup stGroupFile = new STGroupFile("stg/graphql/Directive.stg");

    private String name;
    private List<InputValue> arguments;
    private List<String> directiveLocations;
    private String description;

    public String getName() {
        return name;
    }

    public Directive setName(String name) {
        this.name = name;
        return this;
    }

    public List<InputValue> getArguments() {
        return arguments;
    }

    public Directive setArguments(List<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public List<String> getDirectiveLocations() {
        return directiveLocations;
    }

    public Directive setDirectiveLocations(List<String> directiveLocations) {
        this.directiveLocations = directiveLocations;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Directive setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("directiveDefinition");
        st.add("directive", this);
        return st.render();
    }
}
