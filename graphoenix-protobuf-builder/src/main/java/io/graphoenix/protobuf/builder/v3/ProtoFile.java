package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class ProtoFile {

    private String pkg;

    private List<Import> imports;

    private List<String> topLevelDefs;

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

    public ProtoFile setTopLevelDefs(List<String> topLevelDefs) {
        this.topLevelDefs = topLevelDefs;
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
