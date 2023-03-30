package io.graphoenix.core.introspection;

import io.graphoenix.core.operation.Arguments;

import java.util.LinkedHashSet;
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

    public void addTypes(Set<__Type> types) {
        if (this.types == null) {
            this.types = new LinkedHashSet<>();
        }
        this.types.addAll(types);
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

    public Arguments toArguments() {
        Arguments arguments = new Arguments();
        if (this.getTypes() != null) {
            arguments.put("types", this.getTypes().stream().map(__Type::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getQueryType() != null) {
            arguments.put("queryTypeName", this.getQueryType().getName());
        }
        if (this.getMutationType() != null) {
            arguments.put("mutationTypeName", this.getMutationType().getName());
        }
        if (this.getSubscriptionType() != null) {
            arguments.put("subscriptionTypeName", this.getSubscriptionType().getName());
        }
        if (this.getDirectives() != null) {
            arguments.put("directives", this.getDirectives().stream().map(__Directive::toObjectValue).collect(Collectors.toList()));
        }
        if (this.getDescription() != null) {
            arguments.put("description", this.getDescription());
        }
        return arguments;
    }
}
