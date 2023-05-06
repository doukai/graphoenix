package io.graphoenix.core.error;

import io.graphoenix.spi.dto.GraphQLError;
import io.graphoenix.spi.dto.GraphQLLocation;
import org.eclipse.microprofile.graphql.GraphQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphQLErrors extends RuntimeException {

    private Object data = null;

    private List<GraphQLError> errors = new ArrayList<>();

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

    public GraphQLErrors(GraphQLErrors graphQLErrors) {
        this.data = graphQLErrors.getData();
        this.errors = graphQLErrors.getErrors();
    }

    public GraphQLErrors(GraphQLException graphQLException) {
        this.data = graphQLException.getPartialResults();
        this.errors.add(new GraphQLError(graphQLException.getMessage()));
    }

    public GraphQLErrors(Throwable throwable) {
        addThrowable(throwable);
    }

    public GraphQLErrors(String message) {
        this.errors.add(new GraphQLError(message));
    }

    public void addThrowable(Throwable throwable) {
        if (throwable.getCause() != null) {
            addThrowable(throwable.getCause());
        } else if (throwable instanceof GraphQLErrors) {
            this.data = ((GraphQLErrors) throwable).getData();
            this.errors = ((GraphQLErrors) throwable).getErrors();
        } else if (throwable instanceof GraphQLException) {
            this.data = ((GraphQLException) throwable).getPartialResults();
            this.errors.add(new GraphQLError(throwable.getMessage()));
        } else {
            this.errors.add(new GraphQLError(throwable.getMessage()));
        }
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

    @Override
    public String getMessage() {
        return this.errors.stream().map(GraphQLError::getMessage).collect(Collectors.joining("\r\n"));
    }

    @Override
    public String toString() {
        return "GraphQLErrors{" +
                "data=" + data +
                ", errors=" + errors +
                '}';
    }
}
