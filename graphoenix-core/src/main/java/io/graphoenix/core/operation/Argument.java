package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Argument {

    private String name;
    private String valueWithVariable;

    public Argument() {
    }

    public Argument(GraphqlParser.ArgumentContext argumentContext) {
        this.name = argumentContext.name().getText();
        this.valueWithVariable = new ValueWithVariable(argumentContext.valueWithVariable()).toString();
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

    public Argument setValueWithVariable(NullValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(BooleanValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(IntValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(FloatValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(StringValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(EnumValue value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(ObjectValueWithVariable value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(ArrayValueWithVariable value) {
        this.valueWithVariable = value.toString();
        return this;
    }

    public Argument setValueWithVariable(Variable variable) {
        this.valueWithVariable = variable.toString();
        return this;
    }

    public Argument setValueWithVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        this.valueWithVariable = new ValueWithVariable(valueWithVariableContext).toString();
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Argument.stg");
        ST st = stGroupFile.getInstanceOf("argumentDefinition");
        st.add("argument", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
