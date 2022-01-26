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
        STGroupFile stGroupFile = new STGroupFile("stg/operation/Variable.stg");
        ST st = stGroupFile.getInstanceOf("variableDefinition");
        st.add("variable", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
