package io.graphoenix.spi.error;

public enum ElementErrorType {
    EXPRESSION_VALUE_OR_VARIABLE_FIELD_NOT_EXIST(-201, "value or variable field not exist in expression annotation: %s"),
    EXPRESSION_VALUE_OR_VARIABLE_NOT_EXIST(-202, "value or variable not exist in expression annotation: %s"),
    EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST(-203, "variable parameter: %s not not exist in method: %s"),
    EXPRESSION_EXPRESSIONS_FIELD_NOT_EXIST(-204, "expressions field not exist in expression annotation: %s"),
    EXPRESSION_OPERATOR_NOT_EXIST(-205, "operator not exist in expression annotation: %s"),
    EXPRESSIONS_CONDITIONAL_NOT_EXIST(-206, "conditional not exist in expressions annotation: %s"),

    SOURCE_ANNOTATION_NOT_EXIST(-207, "@Source annotation not exist in api method: %s"),

    UNKNOWN(-299, "unknown element error");

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
