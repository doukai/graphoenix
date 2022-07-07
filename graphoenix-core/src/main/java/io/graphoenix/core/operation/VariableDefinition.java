package io.graphoenix.core.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class VariableDefinition {

    private Variable variable;
    private String typeName;
    private String defaultValue;
    private Set<String> directives;

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

    public String getDefaultValue() {
        return defaultValue;
    }

    public VariableDefinition setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public VariableDefinition setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/VariableDefinition.stg");
        ST st = stGroupFile.getInstanceOf("variableDefinitionDefinition");
        st.add("variableDefinition", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
