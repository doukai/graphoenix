package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class BooleanValue extends ValueWithVariable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/BooleanValue.stg");

    private Boolean value;

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("booleanValueDefinition");
        st.add("booleanValue", this);
        return st.render();
    }
}
