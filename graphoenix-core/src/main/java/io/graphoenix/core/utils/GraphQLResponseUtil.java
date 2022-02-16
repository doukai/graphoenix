package io.graphoenix.core.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public enum GraphQLResponseUtil {
    GRAPHQL_RESPONSE_UTIL;

    private final GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();

    public String fromJson(JsonElement jsonElement) {
        Map<String, Object> graphQLResponse = new HashMap<>();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        graphQLResponse.put("data", gsonBuilder.create().fromJson(jsonElement, type));
        return gsonBuilder.create().toJson(graphQLResponse);
    }
}
