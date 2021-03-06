package io.graphoenix.spi.dto;

import java.util.List;

public class GraphQLResponse {

    private Object data;

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
