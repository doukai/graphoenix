package io.graphoenix.spi.dto;

import java.util.Collections;
import java.util.List;

public class GraphQLError {

    private String message;

    private List<GraphQLLocation> locations;

    private String path;

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

    public void setMessage(String message) {
        this.message = message;
    }

    public List<GraphQLLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<GraphQLLocation> locations) {
        this.locations = locations;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
