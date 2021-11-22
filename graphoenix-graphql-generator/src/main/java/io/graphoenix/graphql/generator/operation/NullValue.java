package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class NullValue {
    private final STGroup stGroupFile = new STGroupFile("stg/operation/NullValue.stg");

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("nullValueDefinition");
        return st.render();
    }
}
