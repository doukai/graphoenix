package io.graphoenix.graphql.generator.operation;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Argument {

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

    public Argument setValueWithVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        this.valueWithVariable = new ValueWithVariable(valueWithVariableContext).toString();
        return this;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/Argument.stg").getInstanceOf("argumentDefinition");
        st.add("argument", this);
        return st.render();
    }
}
