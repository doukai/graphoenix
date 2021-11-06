package io.graphoenix.spi.error;

public enum GraphQLErrorType {
    QUERY_NOT_EXIST(-1, "query not exist in operation"),
    MUTATION_NOT_EXIST(-2, "mutation not exist in operation"),
    SUBSCRIBE_NOT_EXIST(-3, "subscribe not exist in operation"),
    FRAGMENT_NOT_EXIST(-4, "fragment not exist in document and operation"),
    MAP_FROM_NOT_EXIST(-4, "map from argument not exist in @map directive"),
    MAP_TO_NOT_EXIST(-4, "map to argument not exist in @map directive"),
    MAP_WITH_TYPE_NOT_EXIST(-4, "map with type not exist: %s"),
    MAP_WITH_FROM_FIELD_EXIST(-4, "map with from field not exist: %s"),
    MAP_WITH_TO_FIELD_EXIST(-4, "map with to field not exist: %s");

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

    public GraphQLErrorType setVariables(Object... variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public String toString() {
        return code + ": " + String.format(description, variables);
    }
}
