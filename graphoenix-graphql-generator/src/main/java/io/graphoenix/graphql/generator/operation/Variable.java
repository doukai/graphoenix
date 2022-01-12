package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Variable {

    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/Variable.stg").getInstanceOf("variableDefinition");
        st.add("variable", this);
        return st.render();
    }
}
