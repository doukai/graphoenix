package io.graphoenix.core.dto;

import com.dslplatform.json.CompiledJson;
import jakarta.json.JsonValue;

import java.util.Map;

@CompiledJson
public class GraphQLRequest {

    private String query;

    private String operationName;

    private Map<String, JsonValue> variables;

    public GraphQLRequest() {
    }

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public GraphQLRequest(String query, String operationName, Map<String, JsonValue> variables) {
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

    public Map<String, JsonValue> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, JsonValue> variables) {
        this.variables = variables;
    }
}
