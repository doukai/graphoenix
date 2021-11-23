package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/ArrayValueWithVariable.stg");

    private Collection<?> valueWithVariables;


    public ArrayValueWithVariable(Arrays valueWithVariables) {
        this.valueWithVariables = Stream.of(valueWithVariables).map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public ArrayValueWithVariable(Collection<?> valueWithVariables) {
        this.valueWithVariables = valueWithVariables.stream().map(ValueWithVariable::new).collect(Collectors.toList());
    }

    public Collection<?> getValueWithVariables() {
        return valueWithVariables;
    }

    public void setValueWithVariables(Collection<?> valueWithVariables) {
        this.valueWithVariables = valueWithVariables;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("arrayValueWithVariableDefinition");
        st.add("valueWithVariables", valueWithVariables.stream().map(Object::toString).collect(Collectors.toList()));
        return st.render();
    }
}
