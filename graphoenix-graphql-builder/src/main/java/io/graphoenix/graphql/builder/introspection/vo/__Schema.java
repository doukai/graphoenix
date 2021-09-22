package io.graphoenix.graphql.builder.introspection.vo;

import java.util.List;

public class __Schema {

    private List<__Type> types;

    private __Type queryType;

    private __Type mutationType;

    private __Type subscriptionType;

    private List<__Directive> directives;

    private boolean hasMutationType;

    private boolean hasSubscriptionType;

    private boolean hasDirectives;

    public List<__Type> getTypes() {
        return types;
    }

    public __Schema setTypes(List<__Type> types) {
        this.types = types;
        return this;
    }

    public __Type getQueryType() {
        return queryType;
    }

    public __Schema setQueryType(__Type queryType) {
        this.queryType = queryType;
        return this;
    }

    public __Type getMutationType() {
        return mutationType;
    }

    public __Schema setMutationType(__Type mutationType) {
        this.mutationType = mutationType;
        return this;
    }

    public __Type getSubscriptionType() {
        return subscriptionType;
    }

    public __Schema setSubscriptionType(__Type subscriptionType) {
        this.subscriptionType = subscriptionType;
        return this;
    }

    public List<__Directive> getDirectives() {
        return directives;
    }

    public __Schema setDirectives(List<__Directive> directives) {
        this.directives = directives;
        return this;
    }

    public boolean isHasMutationType() {
        return hasMutationType;
    }

    public __Schema setHasMutationType(boolean hasMutationType) {
        this.hasMutationType = hasMutationType;
        return this;
    }

    public boolean isHasSubscriptionType() {
        return hasSubscriptionType;
    }

    public __Schema setHasSubscriptionType(boolean hasSubscriptionType) {
        this.hasSubscriptionType = hasSubscriptionType;
        return this;
    }

    public boolean isHasDirectives() {
        return hasDirectives;
    }

    public __Schema setHasDirectives(boolean hasDirectives) {
        this.hasDirectives = hasDirectives;
        return this;
    }
}
