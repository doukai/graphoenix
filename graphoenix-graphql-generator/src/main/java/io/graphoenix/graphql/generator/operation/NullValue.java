package io.graphoenix.graphql.generator.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class NullValue {

    @Override
    public String toString() {
        ST st = new STGroupFile("stg/operation/NullValue.stg").getInstanceOf("nullValueDefinition");
        return st.render();
    }
}
