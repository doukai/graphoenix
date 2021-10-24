package io.graphoenix.r2dbc.connector.dto;

public class SelectionResult {
    private final String name;
    private final String value;

    public SelectionResult(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SelectionResult{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
