package io.graphoenix.core.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class __Field {

    private String name;

    private String description;

    private Set<__InputValue> args;

    private __Type type;

    private Boolean isDeprecated = false;

    private String deprecationReason;

    private String from;

    private String to;

    private String withType;

    private String withFrom;

    private String withTo;

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

    public Set<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(Set<__InputValue> args) {
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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getWithType() {
        return withType;
    }

    public void setWithType(String withType) {
        this.withType = withType;
    }

    public String getWithFrom() {
        return withFrom;
    }

    public void setWithFrom(String withFrom) {
        this.withFrom = withFrom;
    }

    public String getWithTo() {
        return withTo;
    }

    public void setWithTo(String withTo) {
        this.withTo = withTo;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__Field.stg");
        ST st = stGroupFile.getInstanceOf("__fieldDefinition");
        st.add("__field", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
