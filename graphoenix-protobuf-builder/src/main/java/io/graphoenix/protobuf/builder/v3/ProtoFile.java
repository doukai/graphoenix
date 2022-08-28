package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class ProtoFile {

    private String pkg;

    private List<Import> imports;

    private List<String> topLevelDefs;

    private List<Option> options;

    public String getPkg() {
        return pkg;
    }

    public ProtoFile setPkg(String pkg) {
        this.pkg = pkg;
        return this;
    }

    public List<Import> getImports() {
        return imports;
    }

    public ProtoFile setImports(List<Import> imports) {
        this.imports = imports;
        return this;
    }

    public List<String> getTopLevelDefs() {
        return topLevelDefs;
    }

    public ProtoFile addTopLevelDef(String topLevelDef) {
        if (this.topLevelDefs == null) {
            this.topLevelDefs = new ArrayList<>();
        }
        this.topLevelDefs.add(topLevelDef);
        return this;
    }

    public ProtoFile setTopLevelDefs(List<String> topLevelDefs) {
        this.topLevelDefs = topLevelDefs;
        return this;
    }

    public List<Option> getOptions() {
        return options;
    }

    public ProtoFile setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/ProtoFile.stg");
        ST st = stGroupFile.getInstanceOf("protoFileDefinition");
        st.add("protoFile", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
