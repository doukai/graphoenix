package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class Argument {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/Argument.stg");

    private String name;
    private String valueWithVariable;

    public Argument() {
    }

    public Argument(String name, String valueWithVariable) {
        this.name = name;
        this.valueWithVariable = valueWithVariable;
    }

    public String getName() {
        return name;
    }

    public Argument setName(String name) {
        this.name = name;
        return this;
    }

    public String getValueWithVariable() {
        return valueWithVariable;
    }

    public Argument setValueWithVariable(String valueWithVariable) {
        this.valueWithVariable = valueWithVariable;
        return this;
    }

    public Argument setValueWithVariable(Object object) {
        this.valueWithVariable = new ValueWithVariable(object).toString();
        return this;
    }

    public Argument setValueWithVariable(ValueWithVariable valueWithVariable) {
        this.valueWithVariable = valueWithVariable.toString();
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("argumentDefinition");
        st.add("argument", this);
        return st.render();
    }
}
