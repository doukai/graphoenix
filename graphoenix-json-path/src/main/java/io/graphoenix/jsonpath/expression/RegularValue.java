package io.graphoenix.jsonpath.expression;

public class RegularValue implements Expression {
    private final String value;

    public RegularValue(String value) {
        if (value.startsWith("%")) {
            value = value.replaceFirst("%", "^.*");
        }
        if (value.endsWith("%")) {
            value = value.substring(0, value.length() - 1) + ".*$";
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return "/" + value + "/";
    }
}
