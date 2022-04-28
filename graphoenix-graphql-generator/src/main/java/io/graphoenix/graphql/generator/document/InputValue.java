package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class InputValue {

    private String name;
    private String typeName;
    private String defaultValue;
    private Set<String> directives;
    private String description;

    public InputValue() {
    }

    public InputValue(String name) {
        this.name = name;
    }

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
        if (defaultValue != null) {
            if (this.getTypeName() != null && (this.getTypeName().equals("String") || this.getTypeName().equals("String!"))) {
                this.defaultValue = "\"".concat(defaultValue).concat("\"");
            } else {
                this.defaultValue = defaultValue;
            }
        }
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public InputValue setDirectives(Set<String> directives) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/InputValue.stg");
        ST st = stGroupFile.getInstanceOf("inputValueDefinition");
        st.add("inputValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
