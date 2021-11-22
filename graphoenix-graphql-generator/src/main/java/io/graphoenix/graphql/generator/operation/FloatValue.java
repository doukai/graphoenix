package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class FloatValue {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/FloatValue.stg");

    private Number value;

    public FloatValue(Number value) {
        this.value = value;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("floatValueDefinition");
        st.add("floatValue", this);
        return st.render();
    }
}
