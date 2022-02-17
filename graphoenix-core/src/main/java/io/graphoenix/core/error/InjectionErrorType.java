package io.graphoenix.core.error;

public enum InjectionErrorType {
    BEAN_NOT_EXIST(-101, "bean instance not exist: %s name: %s"),

    UNKNOWN(-199, "unknown error");

    private final int code;
    private final String description;
    private Object[] variables;

    InjectionErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public InjectionErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
