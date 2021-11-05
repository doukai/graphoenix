package io.graphoenix.graphql.generator.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class __Field {

    private final STGroup stGroupFile = new STGroupFile("stg/introspection/__Field.stg");

    private String name;

    private String description;

    private List<__InputValue> args;

    private __Type type;

    private Boolean isDeprecated = false;

    private String deprecationReason;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(List<__InputValue> args) {
        this.args = args;
    }

    public __Type getType() {
        return type;
    }

    public void setType(__Type type) {
        this.type = type;
    }

    public Boolean getIsDeprecated() {
        return isDeprecated;
    }

    public void setIsDeprecated(Boolean deprecated) {
        isDeprecated = deprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("__fieldDefinition");
        st.add("__field", this);
        return st.render();
    }
}
