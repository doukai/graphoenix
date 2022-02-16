package io.graphoenix.spi.error;

public enum ElementErrorType {
    METHOD_PARAMETER_NOT_EXIST(-201, "parameter: %s not not exist in method: %s"),

    UNKNOWN(-299, "unknown error");

    private final int code;
    private final String description;
    private Object[] variables;

    ElementErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public ElementErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
