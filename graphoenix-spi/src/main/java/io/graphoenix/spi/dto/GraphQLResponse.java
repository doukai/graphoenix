package io.graphoenix.spi.dto;

import java.util.List;

public class GraphQLResponse {

    private Object data;

    public GraphQLResponse(Object data) {
        this.data = data;
    }

    public GraphQLResponse(Object data, List<GraphQLError> errors) {
        this.data = data;
        this.errors = errors;
    }

    public List<GraphQLError> errors;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
    }
}
