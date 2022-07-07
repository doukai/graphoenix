package io.graphoenix.core.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class __Directive {

    private String name;

    private String description;

    private Set<__DirectiveLocation> locations;

    private Set<__InputValue> args;

    private Boolean onOperation;

    private Boolean onFragment;

    private Boolean onField;

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

    public Set<__DirectiveLocation> getLocations() {
        return locations;
    }

    public void setLocations(Set<__DirectiveLocation> locations) {
        this.locations = locations;
    }

    public Set<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(Set<__InputValue> args) {
        this.args = args;
    }

    public Boolean getOnOperation() {
        return onOperation;
    }

    public void setOnOperation(Boolean onOperation) {
        this.onOperation = onOperation;
    }

    public Boolean getOnFragment() {
        return onFragment;
    }

    public void setOnFragment(Boolean onFragment) {
        this.onFragment = onFragment;
    }

    public Boolean getOnField() {
        return onField;
    }

    public void setOnField(Boolean onField) {
        this.onField = onField;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__Directive.stg");
        ST st = stGroupFile.getInstanceOf("__directiveDefinition");
        st.add("__directive", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
