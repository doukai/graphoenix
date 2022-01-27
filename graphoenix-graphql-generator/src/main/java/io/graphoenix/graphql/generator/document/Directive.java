package io.graphoenix.graphql.generator.document;

import io.graphoenix.graphql.generator.operation.Argument;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;

public class Directive {

    private String name;
    private Set<Argument> arguments;

    public String getName() {
        return name;
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
