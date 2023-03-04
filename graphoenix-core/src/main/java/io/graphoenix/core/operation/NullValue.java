package io.graphoenix.core.operation;

import jakarta.json.JsonValue;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class NullValue implements JsonValue {

    @Override
    public ValueType getValueType() {
        return ValueType.NULL;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/operation/NullValue.stg");
        ST st = stGroupFile.getInstanceOf("nullValueDefinition");
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
