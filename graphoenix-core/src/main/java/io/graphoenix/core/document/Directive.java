package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Argument;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Directive {

    private String name;
    private Set<Argument> arguments;

    public String getName() {
        return name;
    }

    public Directive() {
    }

    public Directive(GraphqlParser.DirectiveContext directiveContext) {
        this.name = directiveContext.name().getText();
        this.arguments = directiveContext.arguments().argument().stream().map(Argument::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Directive(String name) {
        this.name = name;
    }

    public Directive setName(String name) {
        this.name = name;
        return this;
    }

    public Set<Argument> getArguments() {
        return arguments;
    }

    public Directive setArguments(Set<Argument> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Directive addArgument(Argument argument) {
        if (arguments == null) {
            arguments = new LinkedHashSet<>();
        }
        this.arguments.add(argument);
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
