package io.graphoenix.protobuf.builder.v3;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Rpc {

    private String name;

    private Boolean messageStream;

    private String messageType;

    private Boolean returnStream;

    private String returnType;

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
