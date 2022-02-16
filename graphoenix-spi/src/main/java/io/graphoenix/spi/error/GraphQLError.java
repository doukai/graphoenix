package io.graphoenix.spi.error;

import io.graphoenix.spi.dto.GraphQLLocation;
import io.graphoenix.spi.dto.GraphQLPath;

import java.util.List;

public class GraphQLError {

    private String message;

    private List<GraphQLLocation> locations;

    private GraphQLPath path;

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

    public GraphQLPath getPath() {
        return path;
    }

    public void setPath(GraphQLPath path) {
        this.path = path;
    }
}
