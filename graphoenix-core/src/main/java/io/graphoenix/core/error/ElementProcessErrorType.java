package io.graphoenix.core.error;

public enum ElementProcessErrorType {

    SOURCE_ANNOTATION_NOT_EXIST(-201, "@Source annotation not exist in api method parameters: %s"),
    INVOKE_METHOD_NOT_EXIST(-202, "%s %s invoke method not exist"),

    OPERATION_ANNOTATION_NOT_EXIST(-211, "@GraphQLOperation annotation not exist in operation interface: %s"),
    OPERATION_DAO_VALUE_NOT_EXIST(-212, "operationDAO value not exist in @GraphQLOperation"),
    EXPRESSION_VALUE_OR_VARIABLE_FIELD_NOT_EXIST(-213, "value or variable field not exist in expression annotation: %s"),
    EXPRESSION_VALUE_OR_VARIABLE_NOT_EXIST(-214, "value or variable not exist in expression annotation: %s"),
    EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST(-215, "variable parameter: %s not not exist in method: %s"),
    EXPRESSION_EXPRESSIONS_FIELD_NOT_EXIST(-216, "expressions field not exist in expression annotation: %s"),
    EXPRESSION_OPERATOR_NOT_EXIST(-217, "operator not exist in expression annotation: %s"),
    EXPRESSIONS_CONDITIONAL_NOT_EXIST(-218, "conditional not exist in expressions annotation: %s"),
    UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE(-219, "unsupported operation method return type: %s"),

    UNKNOWN(-299, "unknown element error");

    private final int code;
    private final String description;
    private Object[] variables;

    ElementProcessErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public ElementProcessErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
