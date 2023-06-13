package io.graphoenix.jsonpath.translator.expression;

public class StringValue implements Expression {
    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
