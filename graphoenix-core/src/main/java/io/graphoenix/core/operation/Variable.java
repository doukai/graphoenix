package io.graphoenix.core.operation;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonString;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Variable implements ValueWithVariable, JsonString {

    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public Variable(GraphqlParser.VariableContext variableContext) {
        this.name = variableContext.name().getText();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getString() {
        return toString();
    }

    @Override
    public CharSequence getChars() {
        return toString();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.STRING;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Variable.stg");
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
