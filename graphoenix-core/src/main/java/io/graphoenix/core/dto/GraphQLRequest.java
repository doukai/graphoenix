package io.graphoenix.core.dto;

import com.dslplatform.json.CompiledJson;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Map;

@CompiledJson
public class GraphQLRequest {

    private String query;

    private String operationName;

    private JsonObject variables;

    public GraphQLRequest() {
    }

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public GraphQLRequest(String query, String operationName, JsonObject variables) {
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

    public static GraphQLRequest fromJson(JsonObject jsonObject) {
        return new GraphQLRequest(
                jsonObject.containsKey("query") && !jsonObject.isNull("query") ? jsonObject.getString("query") : null,
                jsonObject.containsKey("operationName") && !jsonObject.isNull("operationName") ? jsonObject.getString("operationName") : null,
                jsonObject.containsKey("variables") && !jsonObject.isNull("variables") ? jsonObject.getJsonObject("variables") : null
        );
    }

    public Map<String, JsonValue> getVariables() {
        if (variables == null) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }
        return variables;
    }

    public void setVariables(JsonObject variables) {
        this.variables = variables;
    }
}
