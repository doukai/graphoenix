package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class ArrayValue extends ValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/ArrayValue.stg");

    private List<ValueWithVariable> valueWithVariables;

    public List<ValueWithVariable> getValueWithVariables() {
        return valueWithVariables;
    }

    public void setValueWithVariables(List<ValueWithVariable> valueWithVariables) {
        this.valueWithVariables = valueWithVariables;
    }

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("arrayValueDefinition");
        st.add("arrayValue", this);
        return st.render();
    }
}
