package io.graphoenix.spi.dto;

import java.util.Map;

public class GraphQLRequest {

    private String query;

    private String operationName;

    private Map<String, String> variables;

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public GraphQLRequest(String query, String operationName, Map<String, String> variables) {
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

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
}
