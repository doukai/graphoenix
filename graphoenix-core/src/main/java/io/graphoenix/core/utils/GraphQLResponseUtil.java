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
        return success(jsonString, null);
    }

    public String success(String jsonString, String id) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonString)).readValue());
        StringWriter stringWriter = new StringWriter();
        if (id != null) {
            jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
            return stringWriter.toString();
        }
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String success(JsonValue jsonValue) {
        return success(jsonValue, null);
    }

    public String success(JsonValue jsonValue, String id) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonValue);
        StringWriter stringWriter = new StringWriter();
        if (id != null) {
            jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
            return stringWriter.toString();
        }
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String success(Object object) {
        return success(object, null);
    }

    public String success(Object object, String id) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(object))).read());
        StringWriter stringWriter = new StringWriter();
        if (id != null) {
            jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
            return stringWriter.toString();
        }
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(GraphQLErrors graphQLErrors) {
        return error(graphQLErrors, null);
    }

    public String error(GraphQLErrors graphQLErrors, String id) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        if (graphQLErrors.getData() != null) {
            responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLErrors.getData()))).read());
        }
        responseBuilder.add("errors", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLErrors.getErrors()))).read());
        StringWriter stringWriter = new StringWriter();
        if (id != null) {
            jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
            return stringWriter.toString();
        }
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(GraphQLException graphQLException) {
        return error(graphQLException, null);
    }

    public String error(GraphQLException graphQLException, String id) {
        JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
        if (graphQLException.getPartialResults() != null) {
            responseBuilder.add("data", jsonProvider.createReader(new StringReader(jsonb.toJson(graphQLException.getPartialResults()))).read());
        }
        JsonArrayBuilder errorsBuilder = jsonProvider.createArrayBuilder();
        errorsBuilder.add(graphQLException.getMessage());
        responseBuilder.add("errors", errorsBuilder);
        StringWriter stringWriter = new StringWriter();
        if (id != null) {
            jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
            return stringWriter.toString();
        }
        jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
        return stringWriter.toString();
    }

    public String error(Throwable throwable) {
        return error(throwable, null);
    }

    public String error(Throwable throwable, String id) {
        if (throwable instanceof GraphQLErrors) {
            return error((GraphQLErrors) throwable, id);
        } else if (throwable instanceof GraphQLException) {
            return error((GraphQLException) throwable, id);
        } else if (throwable.getCause() != null) {
            return error(throwable.getCause(), id);
        } else {
            JsonObjectBuilder responseBuilder = jsonProvider.createObjectBuilder();
            JsonArrayBuilder errorsBuilder = jsonProvider.createArrayBuilder();
            errorsBuilder.add(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
            responseBuilder.add("errors", errorsBuilder);
            StringWriter stringWriter = new StringWriter();
            if (id != null) {
                jsonProvider.createWriter(stringWriter).write(jsonProvider.createObjectBuilder().add("id", id).add("payload", responseBuilder).build());
                return stringWriter.toString();
            }
            jsonProvider.createWriter(stringWriter).write(responseBuilder.build());
            return stringWriter.toString();
        }
    }

    public String next(JsonValue jsonValue) {
        return next(jsonValue, null);
    }

    public String next(JsonValue jsonValue, String id) {
        if (id != null) {
            return "event: next\ndata: " + success(jsonValue, id) + "\n\n";
        } else {
            return "event: next\ndata: " + success(jsonValue) + "\n\n";
        }
    }

    public String next(Throwable throwable) {
        return next(throwable, null);
    }

    public String next(Throwable throwable, String id) {
        if (id != null) {
            return "event: next\ndata: " + error(throwable, id) + "\n\n";
        } else {
            return "event: next\ndata: " + error(throwable) + "\n\n";
        }
    }

    public String complete() {
        return complete(null);
    }

    public String complete(String id) {
        if (id != null) {
            JsonObjectBuilder data = jsonProvider.createObjectBuilder().add("id", id);
            StringWriter stringWriter = new StringWriter();
            jsonProvider.createWriter(stringWriter).write(data.build());
            return "event: complete\ndata: " + stringWriter + "\n\n";
        } else {
            return "event: complete\n\n";
        }
    }
}
