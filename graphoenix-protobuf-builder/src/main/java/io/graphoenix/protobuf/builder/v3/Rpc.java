package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class Rpc {

    private String name;

    private Boolean messageStream;

    private String messageType;

    private Boolean returnStream;

    private String returnType;

    private String description;

    private List<Option> options;

    public String getName() {
        return name;
    }

    public Rpc setName(String name) {
        this.name = name;
        return this;
    }

    public Boolean getMessageStream() {
        return messageStream;
    }

    public Rpc setMessageStream(Boolean messageStream) {
        this.messageStream = messageStream;
        return this;
    }

    public String getMessageType() {
        return messageType;
    }

    public Rpc setMessageType(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public Boolean getReturnStream() {
        return returnStream;
    }

    public Rpc setReturnStream(Boolean returnStream) {
        this.returnStream = returnStream;
        return this;
    }

    public String getReturnType() {
        return returnType;
    }

    public Rpc setReturnType(String returnType) {
        this.returnType = returnType;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Rpc setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Rpc setOptions(List<Option> options) {
        this.options = options;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Rpc.stg");
        ST st = stGroupFile.getInstanceOf("rpcDefinition");
        st.add("rpc", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
