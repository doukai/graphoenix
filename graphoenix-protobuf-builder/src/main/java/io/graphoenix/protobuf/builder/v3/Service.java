package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Service {

    private String name;

    private List<Rpc> rpcs;

    private String description;

    private List<Option> options;

    public String getName() {
        return name;
    }

    public Service setName(String name) {
        this.name = name;
        return this;
    }

    public List<Rpc> getRpcs() {
        return rpcs;
    }

    public Service addRpc(Rpc rpc) {
        if (this.rpcs == null) {
            this.rpcs = new ArrayList<>();
        }
        this.rpcs.add(rpc);
        return this;
    }

    public Service setRpcs(List<Rpc> rpcs) {
        this.rpcs = rpcs;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Service setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Service setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Service.stg");
        ST st = stGroupFile.getInstanceOf("serviceDefinition");
        st.add("service", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
