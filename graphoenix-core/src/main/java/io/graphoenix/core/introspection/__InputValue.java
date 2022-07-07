package io.graphoenix.core.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class __InputValue {

    private String name;

    private String description;

    private __Type type;

    private String defaultValue;

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

    public __Type getType() {
        return type;
    }

    public void setType(__Type type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__InputValue.stg");
        ST st = stGroupFile.getInstanceOf("__inputValueDefinition");
        st.add("__inputValue", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
