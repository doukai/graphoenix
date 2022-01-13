package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class ScalarType {

    private String name;
    private List<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public ScalarType setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public ScalarType setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ScalarType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/document/ScalarType.stg").getInstanceOf("scalarTypeDefinition");
        st.add("scalarType", this);
        return st.render();
    }
}
