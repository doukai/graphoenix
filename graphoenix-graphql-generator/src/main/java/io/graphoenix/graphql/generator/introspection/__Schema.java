package io.graphoenix.graphql.generator.introspection;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class __Schema {

    private List<__Type> types;

    private __Type queryType;

    private __Type mutationType;

    private __Type subscriptionType;

    private List<__Directive> directives;

    private String description;

    public List<__Type> getTypes() {
        return types;
    }

    public void setTypes(List<__Type> types) {
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

    public List<__Directive> getDirectives() {
        return directives;
    }

    public void setDirectives(List<__Directive> directives) {
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
        ST st = new STGroupFile("stg/introspection/__Schema.stg").getInstanceOf("__schemaDefinition");
        st.add("__schema", this);
        return st.render();
    }
}
