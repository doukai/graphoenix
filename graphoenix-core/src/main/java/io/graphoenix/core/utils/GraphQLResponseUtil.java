package io.graphoenix.core.utils;

import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public enum GraphQLResponseUtil {
    GRAPHQL_RESPONSE_UTIL;

    private final GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();

    public String fromJson(String json) {
        Map<String, Object> graphQLResponse = new HashMap<>();
        graphQLResponse.put("data", gsonBuilder.create().fromJson(json, Map.class));
        return gsonBuilder.create().toJson(graphQLResponse);
    }
}
