package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;

import java.util.Set;
import java.util.stream.Collectors;

public class __Directive {

    private String name;

    private String description;

    private Set<__DirectiveLocation> locations;

    private Set<__InputValue> args;

    private Boolean isRepeatable = true;

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

    public Boolean getRepeatable() {
        return isRepeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        isRepeatable = repeatable;
    }

    public ObjectValueWithVariable toObjectValue() {
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
            objectValueWithVariable.put("args", this.getArgs().stream().map(__InputValue::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getRepeatable() != null) {
            objectValueWithVariable.put("isRepeatable", this.getRepeatable());
        }
        return objectValueWithVariable;
    }
}
