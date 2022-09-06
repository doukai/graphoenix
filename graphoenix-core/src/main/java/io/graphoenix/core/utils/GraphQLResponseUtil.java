package io.graphoenix.core.utils;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import org.eclipse.microprofile.graphql.GraphQLException;

import java.io.StringReader;
import java.io.StringWriter;

public enum GraphQLResponseUtil {
    GRAPHQL_RESPONSE_UTIL;

    private final JsonProvider jsonProvider;

    private final Jsonb jsonb;

    GraphQLResponseUtil() {
        this.jsonProvider = BeanContext.get(JsonProvider.class);
        this.jsonb = BeanContext.get(Jsonb.class);
    }

    public String success(String jsonString) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonString)).readValue());
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String success(JsonValue jsonValue) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonValue);
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String success(Object object) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(object))).read());
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(GraphQLErrors graphQLErrors) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        if (graphQLErrors.getData() != null) {
            responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLErrors.getData()))).read());
        }
        responseBuilder.add("errors", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLErrors.getErrors()))).read());
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(GraphQLException graphQLException) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        if (graphQLException.getPartialResults() != null) {
            responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLException.getPartialResults()))).read());
        }
        JsonArrayBuilder errorsBuilder = jsonProvider.createArrayBuilder();
        errorsBuilder.add(graphQLException.getMessage());
        responseBuilder.add("errors", errorsBuilder);
        StringWriter stringWriter = new StringWriter();
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(Throwable throwable) {
        if (throwable instanceof GraphQLErrors) {
            return error((GraphQLErrors) throwable);
        } else if (throwable instanceof GraphQLException) {
            return error((GraphQLException) throwable);
        } else {
            JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
            JsonArrayBuilder errorsBuilder = jsonProvider.createArrayBuilder();
            errorsBuilder.add(throwable.getMessage());
            responseBuilder.add("errors", errorsBuilder);
            StringWriter stringWriter = new StringWriter();
            jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
            return stringWriter.toString();
        }
    }
}
