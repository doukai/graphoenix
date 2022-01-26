package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashSet;
import java.util.Set;

public class Field {

    private String name;
    private Set<InputValue> arguments;
    private String typeName;
    private Set<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public Set<InputValue> getArguments() {
        return arguments;
    }

    public Field setArguments(Set<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArguments(Set<InputValue> arguments) {
        if (this.arguments == null) {
            this.arguments = arguments;
        } else {
            this.arguments.addAll(arguments);
        }
        return this;
    }

    public Field addArgument(InputValue argument) {
        if (this.arguments == null) {
            this.arguments = new HashSet<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public Field setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Field setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/Field.stg");
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
