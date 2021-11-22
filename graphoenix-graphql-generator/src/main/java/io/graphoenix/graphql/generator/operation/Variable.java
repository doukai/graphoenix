package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class Variable extends ValueWithVariable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/Variable.stg");

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        return st.render();
    }
}
