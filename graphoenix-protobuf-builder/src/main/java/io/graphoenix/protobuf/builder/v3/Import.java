package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Import {

    private Boolean pub;

    private Boolean weak;

    private String name;

    public Boolean getPub() {
        return pub;
    }

    public Import setPub(Boolean pub) {
        this.pub = pub;
        return this;
    }

    public Boolean getWeak() {
        return weak;
    }

    public Import setWeak(Boolean weak) {
        this.weak = weak;
        return this;
    }

    public String getName() {
        return name;
    }

    public Import setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Import.stg");
        ST st = stGroupFile.getInstanceOf("importDefinition");
        st.add("import", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
