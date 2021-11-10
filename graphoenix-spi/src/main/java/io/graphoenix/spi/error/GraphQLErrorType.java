package io.graphoenix.spi.error;

public enum GraphQLErrorType {
    OPERATION_NOT_EXIST(-1, "operation not exist"),
    QUERY_NOT_EXIST(-2, "query not exist in operation"),
    MUTATION_NOT_EXIST(-3, "mutation not exist in operation"),
    SUBSCRIBE_NOT_EXIST(-4, "subscribe not exist in operation"),
    FRAGMENT_NOT_EXIST(-5, "fragment not exist: %s -> %s"),
    FIELD_NOT_EXIST(-6, "field not exist: %s -> %s"),
    SELECTION_NOT_EXIST(-11, "selection not exist: %s"),
    TYPE_ID_FIELD_NOT_EXIST(-12, "id field not exist: %s"),
    INPUT_OBJECT_NOT_EXIST(-12, "input object not exist: %s"),
    MAP_DIRECTIVE_NOT_EXIST(-12, "object type field must have @map directive: %s"),
    MAP_FROM_ARGUMENT_NOT_EXIST(-12, "from argument not exist in @map directive: %s"),
    MAP_TO_ARGUMENT_NOT_EXIST(-12, "to argument not exist in @map directive: %s"),
    MAP_FROM_FIELD_NOT_EXIST(-7, "map from field not exist: %s"),
    MAP_TO_OBJECT_NOT_EXIST(-8, "map to object not exist: %s"),
    MAP_TO_FIELD_NOT_EXIST(-8, "map to field not exist: %s"),
    MAP_WITH_TYPE_ARGUMENT_NOT_EXIST(-10, "map with type to field not exist: %s"),
    MAP_WITH_FROM_ARGUMENT_NOT_EXIST(-9, "map with type from field not exist: %s"),
    MAP_WITH_TO_ARGUMENT_NOT_EXIST(-10, "map with type to field not exist: %s"),
    MAP_WITH_TYPE_NOT_EXIST(-10, "map with type to field not exist: %s"),
    MAP_WITH_FROM_FIELD_NOT_EXIST(-9, "map with type from field not exist: %s"),
    MAP_WITH_TO_FIELD_NOT_EXIST(-10, "map with type to field not exist: %s"),
    NONE(-12, "to argument not exist in @map directive: %s");

    private final int code;
    private final String description;
    private Object[] variables;

    private GraphQLErrorType(int code, String description) {
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
