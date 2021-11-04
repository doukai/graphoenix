package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Field {

    private final STGroup stGroupFile = new STGroupFile("stg/document/Field.stg");

    private String name;
    private List<InputValue> arguments;
    private String typeName;
    private List<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public List<InputValue> getArguments() {
        return arguments;
    }

    public Field setArguments(List<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArguments(List<InputValue> arguments) {
        if (this.arguments == null) {
            this.arguments = arguments;
        } else {
            this.arguments.addAll(arguments);
        }
        return this;
    }

    public Field addArgument(InputValue argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
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

    public List<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(List<String> directives) {
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
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        return st.render();
    }
}
