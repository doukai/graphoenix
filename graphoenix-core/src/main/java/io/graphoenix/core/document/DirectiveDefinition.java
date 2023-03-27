package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class DirectiveDefinition {

    private String name;
    private Set<InputValue> arguments;
    private Set<String> directiveLocations;
    private String description;

    public DirectiveDefinition() {
    }

    public DirectiveDefinition(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        this.name = directiveDefinitionContext.name().getText();
        if (directiveDefinitionContext.argumentsDefinition() != null) {
            this.arguments = directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        this.directiveLocations = directiveLocationList(directiveDefinitionContext.directiveLocations());
        if (directiveDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(directiveDefinitionContext.description().StringValue());
        }
    }

    public Set<String> directiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        Set<String> directiveLocationList = new LinkedHashSet<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(directiveLocationsContext.directiveLocation().name().getText());
        } else if (directiveLocationsContext.directiveLocations() != null) {
            directiveLocationList.addAll(directiveLocationList(directiveLocationsContext.directiveLocations()));
        }
        return directiveLocationList;
    }

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
