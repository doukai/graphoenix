package io.graphoenix.graphql.builder.introspection.dto;

public class __DirectiveInputValue {

    private int id;

    private __Directive directive;

    private __InputValue inputValue;

    public int getId() {
        return id;
    }

    public __DirectiveInputValue setId(int id) {
        this.id = id;
        return this;
    }

    public __Directive getDirective() {
        return directive;
    }

    public __DirectiveInputValue setDirective(__Directive directive) {
        this.directive = directive;
        return this;
    }

    public __InputValue getInputValue() {
        return inputValue;
    }

    public __DirectiveInputValue setInputValue(__InputValue inputValue) {
        this.inputValue = inputValue;
        return this;
    }
}
