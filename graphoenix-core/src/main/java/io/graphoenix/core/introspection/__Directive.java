package io.graphoenix.core.introspection;

import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    public ObjectValueWithVariable toValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getName() != null) {
            objectValueWithVariable.put("name", this.getName());
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        if (this.getLocations() != null) {
            objectValueWithVariable.put("locations", this.getLocations());
        }
        if (this.getArgs() != null) {
            objectValueWithVariable.put("args", this.getArgs().stream().map(__InputValue::toValue).collect(Collectors.toList()));
        }
        if (this.getOnOperation() != null) {
            objectValueWithVariable.put("onOperation", this.getOnOperation());
        }
        if (this.getOnFragment() != null) {
            objectValueWithVariable.put("onFragment", this.getOnFragment());
        }
        if (this.getOnField() != null) {
            objectValueWithVariable.put("onField", this.getOnField());
        }
        return objectValueWithVariable;
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
