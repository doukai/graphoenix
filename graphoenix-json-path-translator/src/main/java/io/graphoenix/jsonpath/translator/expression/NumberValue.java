package io.graphoenix.jsonpath.translator.expression;

public class NumberValue implements Expression {
    private final String value;

    public NumberValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
