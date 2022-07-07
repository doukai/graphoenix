package io.graphoenix.core.operation;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class NullValue {

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/NullValue.stg");
        ST st = stGroupFile.getInstanceOf("nullValueDefinition");
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}