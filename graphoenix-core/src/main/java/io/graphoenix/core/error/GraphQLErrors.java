package io.graphoenix.core.error;

import io.graphoenix.spi.dto.GraphQLError;
import io.graphoenix.spi.dto.GraphQLLocation;
import org.eclipse.microprofile.graphql.GraphQLException;

import java.util.ArrayList;
import java.util.List;

public class GraphQLErrors extends RuntimeException {

    private Object data = null;

    private final List<GraphQLError> errors = new ArrayList<>();

    public GraphQLErrors() {
    }

    public GraphQLErrors(GraphQLErrorType graphQLErrorType) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString()));
    }

    public GraphQLErrors(GraphQLErrorType graphQLErrorType, int line, int column) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), line, column));
    }

    public GraphQLErrors(Object data, GraphQLErrorType graphQLErrorType) {
        this.data = data;
        this.errors.add(new GraphQLError(graphQLErrorType.toString()));
    }

    public GraphQLErrors(Object data, GraphQLErrorType graphQLErrorType, int line, int column) {
        this.data = data;
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), line, column));
    }

    public GraphQLErrors(GraphQLException graphQLException) {
        this.data = graphQLException.getPartialResults();
        this.errors.add(new GraphQLError(graphQLException.getMessage()));
    }

    public GraphQLErrors(Exception exception) {
        this.errors.add(new GraphQLError(exception.getMessage()));
    }

    public GraphQLErrors add(GraphQLErrorType graphQLErrorType) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString()));
        return this;
    }

    public GraphQLErrors add(GraphQLErrorType graphQLErrorType, List<GraphQLLocation> locations, String path) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), locations, path));
        return this;
    }

    public GraphQLErrors add(GraphQLErrorType graphQLErrorType, List<GraphQLLocation> locations) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), locations));
        return this;
    }

    public GraphQLErrors add(GraphQLErrorType graphQLErrorType, int line, int column) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), line, column));
        return this;
    }

    public GraphQLErrors add(GraphQLErrorType graphQLErrorType, String path) {
        this.errors.add(new GraphQLError(graphQLErrorType.toString(), path));
        return this;
    }

    public GraphQLErrors add(GraphQLError graphQLError) {
        this.errors.add(graphQLError);
        return this;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }
}
