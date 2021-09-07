package io.graphoenix.graphql.builder.introspection.dto;

public class __FieldInputValue {

    private int id;

    private __Field field;

    private __InputValue inputValue;

    public int getId() {
        return id;
    }

    public __FieldInputValue setId(int id) {
        this.id = id;
        return this;
    }

    public __Field getField() {
        return field;
    }

    public __FieldInputValue setField(__Field field) {
        this.field = field;
        return this;
    }

    public __InputValue getInputValue() {
        return inputValue;
    }

    public __FieldInputValue setInputValue(__InputValue inputValue) {
        this.inputValue = inputValue;
        return this;
    }
}
