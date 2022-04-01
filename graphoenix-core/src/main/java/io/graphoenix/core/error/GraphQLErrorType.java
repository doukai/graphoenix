package io.graphoenix.core.error;

public enum GraphQLErrorType {
    DEFINITION_NOT_EXIST(-1, "definition not exist"),
    TYPE_DEFINITION_NOT_EXIST(-2, "type definition not exist"),

    QUERY_TYPE_NOT_EXIST(-11, "query type definition not exist"),
    MUTATION_TYPE_NOT_EXIST(-12, "mutation type definition not exist"),
    SUBSCRIBE_TYPE_NOT_EXIST(-13, "subscribe type definition not exist"),
    TYPE_NOT_EXIST(-14, "type definition not exist: %s"),
    INPUT_OBJECT_NOT_EXIST(-15, "input object definition not exist: %s"),
    META_INTERFACE_NOT_EXIST(-16, "meta interface definition not exist"),

    FIELD_NOT_EXIST(-21, "field definition not exist in type %s: %s"),
    TYPE_ID_FIELD_NOT_EXIST(-22, "input object definition not exist: %s"),
    FRAGMENT_NOT_EXIST(-23, "fragment not exist: %s"),

    QUERY_NOT_EXIST(-31, "query not exist in operation"),
    MUTATION_NOT_EXIST(-32, "mutation not exist in operation"),
    SUBSCRIBE_NOT_EXIST(-33, "subscribe not exist in operation"),
    OPERATION_NOT_EXIST(-34, "operation not exist"),
    OPERATION_VARIABLE_NOT_EXIST(-35, "variable: %s not exist in operation: %s"),
    SELECTION_NOT_EXIST(-36, "selection definition not exist: %s"),
    OBJECT_SELECTION_NOT_EXIST(-37, "selection definition not exist in object type field: %s"),
    SELECTION_ARGUMENT_NOT_EXIST(-38, "argument: %s definition not exist in selection: %s"),
    NON_NULL_VALUE_NOT_EXIST(-39, "non null value not exist: %s"),

    UNSUPPORTED_OPERATION_TYPE(-41, "unsupported operation type"),
    UNSUPPORTED_FIELD_TYPE(-42, "unsupported field type: %s"),
    UNSUPPORTED_VALUE(-43, "unsupported field value: %s"),
    UNSUPPORTED_OPERATOR(-43, "unsupported operator value: %s simple:(file:{opr:GT, val:\"graphoenix\"})"),
    UNSUPPORTED_FUNCTION_NAME(-44, "unsupported function name: %s"),

    MAP_DIRECTIVE_NOT_EXIST(-51, "object type field must have @map directive: %s"),
    MAP_FROM_ARGUMENT_NOT_EXIST(-52, "from argument not exist in @map directive: %s"),
    MAP_TO_ARGUMENT_NOT_EXIST(-53, "to argument not exist in @map directive: %s"),
    MAP_FROM_FIELD_NOT_EXIST(-54, "map from field not exist: %s"),
    MAP_TO_OBJECT_NOT_EXIST(-55, "map to object not exist: %s"),
    MAP_TO_FIELD_NOT_EXIST(-56, "map to field not exist: %s"),
    MAP_WITH_TYPE_ARGUMENT_NOT_EXIST(-57, "map with type to field not exist: %s"),
    MAP_WITH_FROM_ARGUMENT_NOT_EXIST(-58, "map with type from field not exist: %s"),
    MAP_WITH_TO_ARGUMENT_NOT_EXIST(-59, "map with type to field not exist: %s"),
    MAP_WITH_TYPE_NOT_EXIST(-60, "map with type not exist: %s"),
    MAP_WITH_FROM_FIELD_NOT_EXIST(-61, "map with type from field not exist: %s"),
    MAP_WITH_TO_FIELD_NOT_EXIST(-62, "map with type to field not exist: %s"),

    CLASS_NAME_ARGUMENT_NOT_EXIST(-71, "className not exist in @invoke directive: %s"),
    METHOD_NAME_ARGUMENT_NOT_EXIST(-72, "methodName not exist in @invoke directive: %s"),

    FUNC_NAME_NOT_EXIST(-81, "@func name argument not exist in function filed: %s"),
    FUNC_FIELD_NOT_EXIST(-82, "@func field argument not exist in function filed: %s"),
    CONNECTION_NOT_EXIST(-83, "@connection not exist in filed: %s"),
    CONNECTION_FIELD_NOT_EXIST(-84, "@connection field argument not exist in connection filed: %s"),
    CONNECTION_AGG_FIELD_NOT_EXIST(-85, "@connection agg argument not exist in connection filed: %s"),

    SYNTAX_ERROR(-91, "graphql syntax error: %s line: %s column %s"),

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
