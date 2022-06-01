package io.graphoenix.spi.dto;

import java.util.Collections;
import java.util.List;

public class GraphQLError {

    private String message;

    private List<GraphQLLocation> locations;

    private String path;

    private String schemaPath;

    public GraphQLError() {
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

    public GraphQLError(String message, String path) {
        this.message = message;
        this.path = path;
    }

    public GraphQLError(String message, List<GraphQLLocation> locations, String path) {
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

    public String getPath() {
        return path;
    }

    public GraphQLError setPath(String path) {
        this.path = path;
        return this;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public GraphQLError setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
        return this;
    }
}
