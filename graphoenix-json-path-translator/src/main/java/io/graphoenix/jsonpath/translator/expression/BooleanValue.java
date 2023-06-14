package io.graphoenix.jsonpath.translator.expression;

public class BooleanValue implements Expression {
    private final String value;

    public BooleanValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
