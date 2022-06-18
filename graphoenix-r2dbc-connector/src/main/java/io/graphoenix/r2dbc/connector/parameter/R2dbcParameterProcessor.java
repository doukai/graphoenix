package io.graphoenix.r2dbc.connector.parameter;

import com.google.gson.GsonBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class R2dbcParameterProcessor {

    private final GsonBuilder jsonBuilder = new GsonBuilder();

    public Map<String, Object> process(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), processValue(v.getValue())), HashMap::putAll);
    }

    private Object processValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().isPrimitive() ||
                value instanceof String ||
                value instanceof Character ||
                value instanceof Number ||
                value instanceof Boolean ||
                value instanceof LocalDateTime ||
                value instanceof LocalDate ||
                value instanceof LocalTime) {
            return value;
        } else if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }
        return jsonBuilder.create().toJson(value);
    }
}
