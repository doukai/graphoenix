package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class DirectiveDefinition {

    private String name;
    private List<InputValue> arguments;
    private List<String> directiveLocations;
    private String description;

    public String getName() {
        return name;
    }

    public DirectiveDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public List<InputValue> getArguments() {
        return arguments;
    }

    public DirectiveDefinition setArguments(List<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public List<String> getDirectiveLocations() {
        return directiveLocations;
    }

    public DirectiveDefinition setDirectiveLocations(List<String> directiveLocations) {
        this.directiveLocations = directiveLocations;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DirectiveDefinition setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/document/DirectiveDefinition.stg").getInstanceOf("directiveDefinitionDefinition");
        st.add("directiveDefinition", this);
        return st.render();
    }
}
