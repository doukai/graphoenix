package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class NullValue extends ValueWithVariable {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/NullValue.stg");

    @Override
    public String getValueWithVariable() {
        return this.toString();
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("nullValueDefinition");
        return st.render();
    }
}
