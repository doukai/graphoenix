package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class InputValue {

    private final STGroup stGroupFile = new STGroupFile("stg/document/InputValue.stg");

    private String name;
    private String typeName;
    private String defaultValue;
    private List<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public InputValue setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public InputValue setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public InputValue setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public InputValue setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public InputValue setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("inputValueDefinition");
        st.add("inputValue", this);
        return st.render();
    }
}
