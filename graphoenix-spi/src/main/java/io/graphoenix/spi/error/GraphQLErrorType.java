package io.graphoenix.spi.error;

public enum GraphQLErrorType {
    DEFINITION_NOT_EXIST(-1, "definition not exist"),
    TYPE_DEFINITION_NOT_EXIST(-2, "type definition not exist"),
    OPERATION_NOT_EXIST(-3, "operation not exist"),
    QUERY_NOT_EXIST(-4, "query not exist in operation"),
    MUTATION_NOT_EXIST(-5, "mutation not exist in operation"),
    SUBSCRIBE_NOT_EXIST(-6, "subscribe not exist in operation"),
    FRAGMENT_NOT_EXIST(-7, "fragment not exist: %s"),
    TYPE_NOT_EXIST(-8, "field not exist: %s"),
    TYPE_ID_FIELD_NOT_EXIST(-9, "input object not exist: %s"),
    FIELD_NOT_EXIST(-10, "field not exist in type %s: %s"),
    SELECTION_NOT_EXIST(-11, "selection not exist: %s"),
    INPUT_OBJECT_NOT_EXIST(-12, "input object not exist: %s"),
    NON_NULL_VALUE_NOT_EXIST(-13, "non null value not exist: %s"),

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
    MAP_WITH_TYPE_NOT_EXIST(-25, "map with type to field not exist: %s"),
    MAP_WITH_FROM_FIELD_NOT_EXIST(-26, "map with type from field not exist: %s"),
    MAP_WITH_TO_FIELD_NOT_EXIST(-27, "map with type to field not exist: %s"),

    UNKNOWN(-999, "unknown error");

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
