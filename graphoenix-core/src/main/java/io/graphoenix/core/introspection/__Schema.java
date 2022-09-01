package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.ObjectValueWithVariable;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;
import java.util.stream.Collectors;

public class __Schema {

    private Set<__Type> types;

    private __Type queryType;

    private __Type mutationType;

    private __Type subscriptionType;

    private Set<__Directive> directives;

    private String description;

    public Set<__Type> getTypes() {
        return types;
    }

    public void setTypes(Set<__Type> types) {
        this.types = types;
    }

    public __Type getQueryType() {
        return queryType;
    }

    public void setQueryType(__Type queryType) {
        this.queryType = queryType;
    }

    public __Type getMutationType() {
        return mutationType;
    }

    public void setMutationType(__Type mutationType) {
        this.mutationType = mutationType;
    }

    public __Type getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(__Type subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Set<__Directive> getDirectives() {
        return directives;
    }

    public void setDirectives(Set<__Directive> directives) {
        this.directives = directives;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ObjectValueWithVariable toValue() {
        ObjectValueWithVariable objectValueWithVariable = new ObjectValueWithVariable();
        if (this.getTypes() != null) {
            objectValueWithVariable.put("types", this.getTypes().stream().map(__Type::toValue).collect(Collectors.toList()));
        }
        if (this.getQueryType() != null) {
            objectValueWithVariable.put("queryType", this.getQueryType().toValue());
        }
        if (this.getMutationType() != null) {
            objectValueWithVariable.put("mutationType", this.getMutationType().toValue());
        }
        if (this.getSubscriptionType() != null) {
            objectValueWithVariable.put("subscriptionType", this.getSubscriptionType().toValue());
        }
        if (this.getDirectives() != null) {
            objectValueWithVariable.put("directives", this.getDirectives().stream().map(__Directive::toValue).collect(Collectors.toList()));
        }
        if (this.getDescription() != null) {
            objectValueWithVariable.put("description", this.getDescription());
        }
        return objectValueWithVariable;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/introspection/__Schema.stg");
        ST st = stGroupFile.getInstanceOf("__schemaDefinition");
        st.add("__schema", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
