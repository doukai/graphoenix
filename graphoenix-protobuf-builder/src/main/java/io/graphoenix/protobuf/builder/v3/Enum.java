package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Enum {

    private String name;

    private List<EnumField> fields;

    private List<Option> options;

    public String getName() {
        return name;
    }

    public Enum setName(String name) {
        this.name = name;
        return this;
    }

    public List<EnumField> getFields() {
        return fields;
    }

    public Enum setFields(List<EnumField> fields) {
        this.fields = fields;
        return this;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Enum setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Enum.stg");
        ST st = stGroupFile.getInstanceOf("enumDefinition");
        st.add("enum", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
