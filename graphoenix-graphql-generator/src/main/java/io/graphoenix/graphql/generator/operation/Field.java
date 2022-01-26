package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Field {

    private String name;
    private String alias;
    private Set<Argument> arguments;
    private Set<String> directives;
    private Set<Field> fields;

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

    public Set<Argument> getArguments() {
        return arguments;
    }

    public Field setArguments(Set<Argument> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArgument(Argument argument) {
        if (this.arguments == null) {
            this.arguments = new HashSet<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public Field addArguments(Stream<Argument> argumentStream) {
        if (this.arguments == null) {
            this.arguments = new HashSet<>();
        }
        this.arguments.addAll(argumentStream.collect(Collectors.toList()));
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public Field setFields(Set<Field> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Field.stg");
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
