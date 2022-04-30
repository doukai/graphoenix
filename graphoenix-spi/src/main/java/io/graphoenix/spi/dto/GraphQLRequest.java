package io.graphoenix.spi.dto;

import com.google.gson.JsonElement;

import java.util.Map;

public class GraphQLRequest {

    private String query;

    private String operationName;

    private Map<String, JsonElement> variables;

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public GraphQLRequest(String query, String operationName, Map<String, JsonElement> variables) {
        this.query = query;
        this.operationName = operationName;
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, JsonElement> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, JsonElement> variables) {
        this.variables = variables;
    }
}
