package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class EnumValue extends ValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/EnumValue.stg");

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("enumValueDefinition");
        st.add("enumValue", this);
        return st.render();
    }
}
