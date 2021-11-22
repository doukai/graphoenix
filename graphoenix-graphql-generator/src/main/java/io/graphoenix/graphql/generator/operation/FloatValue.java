package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class FloatValue extends ValueWithVariable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/FloatValue.stg");

    private Float value;

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("floatValueDefinition");
        st.add("floatValue", this);
        return st.render();
    }
}
