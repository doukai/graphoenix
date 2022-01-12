package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class EnumValue {

    private String name;
    private List<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public EnumValue setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public EnumValue setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EnumValue setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/document/EnumValue.stg").getInstanceOf("enumValueDefinition");
        st.add("enumValue", this);
        return st.render();
    }
}
