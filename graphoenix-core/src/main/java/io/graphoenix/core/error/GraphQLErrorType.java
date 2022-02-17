package io.graphoenix.core.error;

public enum GraphQLErrorType {
    DEFINITION_NOT_EXIST(-1, "definition not exist"),
    TYPE_DEFINITION_NOT_EXIST(-2, "type definition not exist"),
    FRAGMENT_NOT_EXIST(-7, "fragment not exist: %s"),
    QUERY_TYPE_NOT_EXIST(-2, "query type definition not exist"),
    MUTATION_TYPE_NOT_EXIST(-2, "mutation type definition not exist"),
    SUBSCRIBE_TYPE_NOT_EXIST(-2, "subscribe type definition not exist"),
    TYPE_NOT_EXIST(-8, "type definition not exist: %s"),
    META_INTERFACE_NOT_EXIST(-8, "meta interface definition not exist"),
    TYPE_ID_FIELD_NOT_EXIST(-9, "input object definition not exist: %s"),
    FIELD_NOT_EXIST(-10, "field definition not exist in type %s: %s"),
    SELECTION_NOT_EXIST(-11, "selection definition not exist: %s"),
    SELECTION_ARGUMENT_NOT_EXIST(-11, "argument: %s definition not exist in selection: %s"),
    INPUT_OBJECT_NOT_EXIST(-12, "input object definition not exist: %s"),

    OPERATION_NOT_EXIST(-3, "operation not exist"),
    OPERATION_VARIABLE_NOT_EXIST(-3, "variable: %s not exist in operation: %s"),
    QUERY_NOT_EXIST(-4, "query not exist in operation"),
    MUTATION_NOT_EXIST(-5, "mutation not exist in operation"),
    SUBSCRIBE_NOT_EXIST(-6, "subscribe not exist in operation"),
    NON_NULL_VALUE_NOT_EXIST(-13, "non null value not exist: %s"),

    UNSUPPORTED_OPERATION_TYPE(-15, "unsupported operation type"),
    UNSUPPORTED_FIELD_TYPE(-14, "unsupported field type: %s"),
    UNSUPPORTED_VALUE(-15, "unsupported field value: %s"),

    MAP_DIRECTIVE_NOT_EXIST(-16, "object type field must have @map directive: %s"),
    MAP_FROM_ARGUMENT_NOT_EXIST(-17, "from argument not exist in @map directive: %s"),
    MAP_TO_ARGUMENT_NOT_EXIST(-18, "to argument not exist in @map directive: %s"),
    MAP_FROM_FIELD_NOT_EXIST(-19, "map from field not exist: %s"),
    MAP_TO_OBJECT_NOT_EXIST(-20, "map to object not exist: %s"),
    MAP_TO_FIELD_NOT_EXIST(-21, "map to field not exist: %s"),
    MAP_WITH_TYPE_ARGUMENT_NOT_EXIST(-22, "map with type to field not exist: %s"),
    MAP_WITH_FROM_ARGUMENT_NOT_EXIST(-23, "map with type from field not exist: %s"),
    MAP_WITH_TO_ARGUMENT_NOT_EXIST(-24, "map with type to field not exist: %s"),
    MAP_WITH_TYPE_NOT_EXIST(-25, "map with type not exist: %s"),
    MAP_WITH_FROM_FIELD_NOT_EXIST(-26, "map with type from field not exist: %s"),
    MAP_WITH_TO_FIELD_NOT_EXIST(-27, "map with type to field not exist: %s"),

    CLASS_NAME_ARGUMENT_NOT_EXIST(-17, "className not exist in @invoke directive: %s"),
    METHOD_NAME_ARGUMENT_NOT_EXIST(-18, "methodName not exist in @invoke directive: %s"),

    SYNTAX_ERROR(-27, "graphql syntax error: %s line: %s column %s"),

    UNKNOWN(-99, "unknown graphql error");

    private final int code;
    private final String description;
    private Object[] variables;

    GraphQLErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public GraphQLErrorType bind(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
