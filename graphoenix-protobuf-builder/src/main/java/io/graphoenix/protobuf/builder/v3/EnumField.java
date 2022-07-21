package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class EnumField {

    private String name;

    private Integer number;

    public String getName() {
        return name;
    }

    public EnumField setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getNumber() {
        return number;
    }

    public EnumField setNumber(Integer number) {
        this.number = number;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/EnumField.stg");
        ST st = stGroupFile.getInstanceOf("enumFieldDefinition");
        st.add("enumField", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
