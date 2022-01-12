package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class VariableDefinition {

    private Variable variable;
    private String typeName;
    private String defaultValue;
    private List<String> directives;

    public Variable getVariable() {
        return variable;
    }

    public VariableDefinition setVariable(Variable variable) {
        this.variable = variable;
        return this;
    }

    public VariableDefinition setVariable(String variableName) {
        this.variable = new Variable(variableName);
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public VariableDefinition setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public VariableDefinition setTypeName(Type type) {
        this.typeName = type.toString();
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
        ST st = new STGroupFile("stg/operation/VariableDefinition.stg").getInstanceOf("variableDefinitionDefinition");
        st.add("variableDefinition", this);
        return st.render();
    }
}
