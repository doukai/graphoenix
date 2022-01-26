package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class ScalarType {

    private String name;
    private Set<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public ScalarType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public ScalarType setDirectives(Set<String> directives) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/ScalarType.stg");
        ST st = stGroupFile.getInstanceOf("scalarTypeDefinition");
        st.add("scalarType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
