package io.graphoenix.antlr.introspection;

import java.util.List;

public class __Schema {

    private List<__Type> types;

    private __Type queryType;

    private __Type mutationType;

    private __Type subscriptionType;

    private List<__Directive> directives;

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
}
