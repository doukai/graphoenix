package io.graphoenix.graphql.builder.schema.dto;

public class GraphqlDirective {

    private String directive;

    private boolean isLast;

    public GraphqlDirective(String directive) {
        this.directive = directive;
    }

    public String getDirective() {
        return directive;
    }

    public GraphqlDirective setDirective(String directive) {
        this.directive = directive;
        return this;
    }

    public boolean isLast() {
        return isLast;
    }

    public GraphqlDirective setLast(boolean last) {
        isLast = last;
        return this;
    }
}
