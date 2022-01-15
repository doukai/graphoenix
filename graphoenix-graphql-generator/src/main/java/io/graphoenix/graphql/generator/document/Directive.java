package io.graphoenix.graphql.generator.document;

import io.graphoenix.graphql.generator.operation.Argument;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Directive {

    private String name;
    private List<Argument> arguments;

    public String getName() {
        return name;
    }

    public Directive setName(String name) {
        this.name = name;
        return this;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Directive setArguments(List<Argument> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Directive addArgument(Argument argument) {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        this.arguments.add(argument);
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/document/Directive.stg").getInstanceOf("directiveDefinition");
        st.add("directive", this);
        return st.render();
    }
}
