package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class Directive {

    private String name;
    private Collection<InputValue> arguments;
    private Collection<String> directiveLocations;
    private String description;

    public Directive() {
    }

    public Directive(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        this.name = directiveDefinitionContext.name().getText();
        if (directiveDefinitionContext.argumentsDefinition() != null) {
            this.arguments = directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream().map(InputValue::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        this.directiveLocations = directiveLocationList(directiveDefinitionContext.directiveLocations());
        if (directiveDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(directiveDefinitionContext.description().StringValue());
        }
    }

    public Collection<String> directiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        Collection<String> directiveLocationList = new LinkedHashSet<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(directiveLocationsContext.directiveLocation().name().getText());
        }
        if (directiveLocationsContext.directiveLocations() != null) {
            directiveLocationList.addAll(directiveLocationList(directiveLocationsContext.directiveLocations()));
        }
        return directiveLocationList;
    }

    public String getName() {
        return name;
    }

    public Directive setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<InputValue> getArguments() {
        return arguments;
    }

    public Directive setArguments(Collection<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Collection<String> getDirectiveLocations() {
        return directiveLocations;
    }

    public Directive setDirectiveLocations(Collection<String> directiveLocations) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/Directive.stg");
        ST st = stGroupFile.getInstanceOf("directiveDefinition");
        st.add("directive", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
