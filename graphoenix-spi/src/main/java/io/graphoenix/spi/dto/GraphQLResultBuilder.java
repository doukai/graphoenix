package io.graphoenix.spi.dto;

import java.util.List;

public class GraphQLResultBuilder {

    private final GraphQLResponse graphQLResponse;

    public GraphQLResultBuilder(Object data) {
        this.graphQLResponse = new GraphQLResponse(data);
    }

    public GraphQLResultBuilder(Object data, Exception exception) {
        this.graphQLResponse = new GraphQLResponse(data);
    }

    public GraphQLResultBuilder(Object data, List<Exception> exceptionList) {
        this.graphQLResponse = new GraphQLResponse(data);
    }

    public GraphQLResponse build() {
        return graphQLResponse;
    }
}
