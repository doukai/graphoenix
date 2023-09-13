package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Option {

    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public Option setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Option setValue(Object value) {
        this.value = String.valueOf(value);
        return this;
    }

    public Option setValue(String value) {
        this.value = "\"" + value + "\"";
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Option.stg");
        ST st = stGroupFile.getInstanceOf("optionDefinition");
        st.add("option", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
