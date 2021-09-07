package io.graphoenix.graphql.builder.introspection.dto;

import java.util.List;

public class __Schema {

    private int id;

    private List<__Type> types;

    private __Type queryType;

    private __Type mutationType;

    private __Type subscriptionType;

    private List<__Directive> directives;

    public int getId() {
        return id;
    }

    public __Schema setId(int id) {
        this.id = id;
        return this;
    }

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
}
