package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class VariableDefinition {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/VariableDefinition.stg");

    private String name;
    private String typeName;
    private String defaultValue;
    private List<String> directives;

    public String getName() {
        return name;
    }

    public VariableDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public VariableDefinition setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public VariableDefinition setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public VariableDefinition setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("variableDefinitionDefinition");
        st.add("variableDefinition", this);
        return st.render();
    }
}
