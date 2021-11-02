package io.graphoenix.spi.dto;

import java.util.List;

public class GraphQLResultBuilder {

    private final GraphQLResult graphQLResult;

    public GraphQLResultBuilder(Object data) {
        this.graphQLResult = new GraphQLResult();
        this.graphQLResult.setData(data);
    }

    public GraphQLResultBuilder(Object data, Exception exception) {
        this.graphQLResult = new GraphQLResult();
        this.graphQLResult.setData(data);
    }

    public GraphQLResultBuilder(Object data, List<Exception> exceptionList) {
        this.graphQLResult = new GraphQLResult();
        this.graphQLResult.setData(data);
    }

    public GraphQLResult build() {
        return graphQLResult;
    }
}
