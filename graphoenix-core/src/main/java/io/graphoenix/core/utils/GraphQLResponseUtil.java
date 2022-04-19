package io.graphoenix.core.utils;

import com.google.gson.*;
import io.graphoenix.core.error.GraphQLErrors;
import org.eclipse.microprofile.graphql.GraphQLException;

public enum GraphQLResponseUtil {
    GRAPHQL_RESPONSE_UTIL;

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    public String success(JsonElement jsonElement) {
        JsonObject response = new JsonObject();
        response.add("data", jsonElement);
        return response.toString();
    }

    public String error(GraphQLErrors graphQLErrors) {
        JsonObject response = new JsonObject();
        if (graphQLErrors.getData() != null) {
            response.add("data", gsonBuilder.create().toJsonTree(graphQLErrors.getData()));
        }
        response.add("errors", gsonBuilder.create().toJsonTree(graphQLErrors.getErrors()));
        return response.toString();
    }

    public String error(GraphQLException graphQLException) {
        JsonObject response = new JsonObject();
        if (graphQLException.getPartialResults() != null) {
            response.add("data", gsonBuilder.create().toJsonTree(graphQLException.getPartialResults()));
        }
        JsonArray errors = new JsonArray();
        errors.add(graphQLException.getMessage());
        response.add("errors", errors);
        return response.toString();
    }

    public String error(Throwable throwable) {
        if (throwable instanceof GraphQLErrors) {
            return error((GraphQLErrors) throwable);
        } else if (throwable instanceof GraphQLException) {
            return error((GraphQLException) throwable);
        } else {
            JsonObject response = new JsonObject();
            JsonArray errors = new JsonArray();
            errors.add(throwable.getMessage());
            response.add("errors", errors);
            return response.toString();
        }
    }
}
