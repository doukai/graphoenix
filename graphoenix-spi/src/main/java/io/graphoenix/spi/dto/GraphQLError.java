package io.graphoenix.spi.dto;

import org.eclipse.microprofile.graphql.GraphQLException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.graphoenix.spi.error.ErrorInfoUtil.ERROR_CODE_UTIL;

public class GraphQLError {

    private String message;

    private List<GraphQLLocation> locations;

    private List<String> path;

    private GraphQLErrorExtensions extensions;

    public GraphQLError() {
    }

    public GraphQLError(GraphQLException graphQLException) {
        this.message = graphQLException.getMessage();
        this.extensions = new GraphQLErrorExtensions(ERROR_CODE_UTIL.getCode(graphQLException.getClass()));
    }

    public GraphQLError(Throwable throwable) {
        this.message = ERROR_CODE_UTIL.getMessage(throwable.getClass());
        this.extensions = new GraphQLErrorExtensions(ERROR_CODE_UTIL.getCode(throwable.getClass()));
    }

    public GraphQLError(Integer code, String message) {
        this.message = message;
        this.extensions = new GraphQLErrorExtensions(code);
    }

    public GraphQLError(String message) {
        this.message = message;
    }

    public GraphQLError(String message, List<GraphQLLocation> locations) {
        this.message = message;
        this.locations = locations;
    }

    public GraphQLError(String message, int line, int column) {
        this.message = message;
        this.locations = Collections.singletonList(new GraphQLLocation(line, column));
    }

    public GraphQLError(String message, List<GraphQLLocation> locations, List<String> path) {
        this.message = message;
        this.locations = locations;
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public GraphQLError setMessage(String message) {
        this.message = message;
        return this;
    }

    public List<GraphQLLocation> getLocations() {
        return locations;
    }

    public GraphQLError setLocations(List<GraphQLLocation> locations) {
        this.locations = locations;
        return this;
    }

    public List<String> getPath() {
        return path;
    }

    public GraphQLError setPath(List<String> path) {
        this.path = path;
        return this;
    }

    public GraphQLError setSchemaPath(String schemaPath) {
        this.path = Arrays.asList(schemaPath.replaceFirst("#/properties/", "").split("/"));
        return this;
    }

    public GraphQLErrorExtensions getExtensions() {
        return extensions;
    }

    public GraphQLError setExtensions(GraphQLErrorExtensions extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        return "GraphQLError{" +
                "message='" + message + '\'' +
                ", locations=" + locations +
                ", path=" + path +
                ", extensions=" + extensions +
                '}';
    }
}
