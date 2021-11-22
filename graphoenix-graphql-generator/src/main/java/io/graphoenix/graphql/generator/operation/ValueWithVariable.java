package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public abstract class ValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/ValueWithVariable.stg");

    private String valueWithVariable;

    public abstract String getValueWithVariable();

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("valueWithVariableDefinition");
        st.add("valueWithVariable", this);
        return st.render();
    }


}
