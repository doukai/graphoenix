package io.graphoenix.graphql.generator.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

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
