package io.graphoenix.jsonpath.translator.expression;

public class BooleanValue implements Expression {
    private final Boolean value;

    public BooleanValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
