package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class IntValue {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/IntValue.stg");

    private Number value;

    public IntValue(Number value) {
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
        ST st = stGroupFile.getInstanceOf("intValueDefinition");
        st.add("intValue", this);
        return st.render();
    }
}
