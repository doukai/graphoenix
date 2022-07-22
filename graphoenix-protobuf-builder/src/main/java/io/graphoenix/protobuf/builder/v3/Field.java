package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Field {

    private Boolean repeated;

    private String name;

    private String type;

    private Integer number;

    private List<Option> options;

    public Boolean getRepeated() {
        return repeated;
    }

    public Field setRepeated(Boolean repeated) {
        this.repeated = repeated;
        return this;
    }

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public Field setType(String type) {
        this.type = type;
        return this;
    }

    public Integer getNumber() {
        return number;
    }

    public Field setNumber(Integer number) {
        this.number = number;
        return this;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Field setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Field.stg");
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("field", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
