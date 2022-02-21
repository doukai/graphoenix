package io.graphoenix.core.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.graphoenix.core.error.GraphQLProblem;

public enum GraphQLResponseUtil {
    GRAPHQL_RESPONSE_UTIL;

    public String success(JsonElement jsonElement) {
        JsonObject response = new JsonObject();
        response.add("data", jsonElement);
        return response.toString();
    }

    public String error(GraphQLProblem problem) {
        return problem.toString();
    }
}
