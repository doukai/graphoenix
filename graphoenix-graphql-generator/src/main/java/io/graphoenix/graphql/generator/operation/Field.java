package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Field {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/Field.stg");

    private String name;
    private String alias;
    private List<Argument> arguments;
    private List<String> directives;
    private List<Field> fields;

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public Field setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Field setArguments(List<Argument> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArgument(Argument argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }

    public Field setFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        return st.render();
    }
}
