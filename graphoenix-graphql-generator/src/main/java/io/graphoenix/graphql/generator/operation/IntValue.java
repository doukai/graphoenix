package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class IntValue extends ValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/IntValue.stg");

    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("intValueDefinition");
        st.add("intValue", this);
        return st.render();
    }
}
