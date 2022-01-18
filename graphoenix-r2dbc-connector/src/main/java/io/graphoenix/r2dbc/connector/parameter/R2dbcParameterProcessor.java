package io.graphoenix.r2dbc.connector.parameter;

import com.google.gson.GsonBuilder;

import java.util.Map;
import java.util.stream.Collectors;

public class R2dbcParameterProcessor {

    private final GsonBuilder jsonBuilder = new GsonBuilder();

    public Map<String, Object> process(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> processValue(entry.getValue())));
    }

    private Object processValue(Object value) {
        if (value.getClass().isPrimitive()) {
            return value;
        } else if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }
        return jsonBuilder.create().toJson(value);
    }
}
