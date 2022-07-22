package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class EnumField {

    private String name;

    private Integer number;

    private List<Option> options;

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

    public List<Option> getOptions() {
        return options;
    }

    public EnumField setOptions(List<Option> options) {
        this.options = options;
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
