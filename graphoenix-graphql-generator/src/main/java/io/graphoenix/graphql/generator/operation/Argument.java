package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Argument {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/Argument.stg");

    private String name;
    private ValueWithVariable valueWithVariable;

    public String getName() {
        return name;
    }

    public Argument setName(String name) {
        this.name = name;
        return this;
    }

    public ValueWithVariable getValueWithVariable() {
        return valueWithVariable;
    }

    public Argument setValueWithVariable(ValueWithVariable valueWithVariable) {
        this.valueWithVariable = valueWithVariable;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("argumentDefinition");
        st.add("argument", this);
        return st.render();
    }
}
