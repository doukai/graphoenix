package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class Variable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/Variable.stg");

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
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        return st.render();
    }
}
