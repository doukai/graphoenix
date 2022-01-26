package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class DirectiveDefinition {

    private String name;
    private Set<InputValue> arguments;
    private Set<String> directiveLocations;
    private String description;

    public String getName() {
        return name;
    }

    public DirectiveDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public Set<InputValue> getArguments() {
        return arguments;
    }

    public DirectiveDefinition setArguments(Set<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Set<String> getDirectiveLocations() {
        return directiveLocations;
    }

    public DirectiveDefinition setDirectiveLocations(Set<String> directiveLocations) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/DirectiveDefinition.stg");
        ST st = stGroupFile.getInstanceOf("directiveDefinitionDefinition");
        st.add("directiveDefinition", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
