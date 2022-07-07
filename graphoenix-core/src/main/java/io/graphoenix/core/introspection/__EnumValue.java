package io.graphoenix.core.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class __EnumValue {

    private String name;

    private String description;

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
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__EnumValue.stg");
        ST st = stGroupFile.getInstanceOf("__enumValueDefinition");
        st.add("__enumValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
