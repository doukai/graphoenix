package io.graphoenix.common.config;

import io.graphoenix.meta.dto.GraphQLResult;

import java.util.List;

public class GraphQLResultBuilder {

    private GraphQLResult graphQLResult;

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
