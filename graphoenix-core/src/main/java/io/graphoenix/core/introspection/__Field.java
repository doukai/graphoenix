package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;
import java.util.stream.Collectors;

public class __Field {

    private String name;

    private String description;

    private Set<__InputValue> args;

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

    public ObjectValueWithVariable toValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getName() != null) {
            objectValueWithVariable.put("name", this.getName());
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        if (this.getArgs() != null) {
            objectValueWithVariable.put("args", this.getArgs().stream().map(__InputValue::toValue).collect(Collectors.toList()));
        }
        if (this.getType() != null) {
            objectValueWithVariable.put("type", this.getType().toValue());
        }
        if (this.getIsDeprecated() != null) {
            objectValueWithVariable.put("isDeprecated", this.getIsDeprecated());
        }
        if (this.getDeprecationReason() != null) {
            objectValueWithVariable.put("deprecationReason", this.getDeprecationReason());
        }
        return objectValueWithVariable;
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
