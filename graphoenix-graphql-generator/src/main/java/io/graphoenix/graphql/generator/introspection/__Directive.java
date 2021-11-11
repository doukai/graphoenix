package io.graphoenix.graphql.generator.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class __Directive {

    private final STGroup stGroupFile = new STGroupFile("stg/introspection/__Directive.stg");

    private String name;

    private String description;

    private List<__DirectiveLocation> locations;

    private List<__InputValue> args;

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

    public List<__DirectiveLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<__DirectiveLocation> locations) {
        this.locations = locations;
    }

    public List<__InputValue> getArgs() {
        return args;
    }

    public void setArgs(List<__InputValue> args) {
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
        ST st = stGroupFile.getInstanceOf("__directiveDefinition");
        st.add("__directive", this);
        return st.render();
    }
}
