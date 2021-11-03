package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Variable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/Variable.stg");

    private String name;
    private String typeName;
    private String defaultValue;
    private List<String> directives;

    public String getName() {
        return name;
    }

    public Variable setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public Variable setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Variable setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public Variable setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        return st.render();
    }
}
