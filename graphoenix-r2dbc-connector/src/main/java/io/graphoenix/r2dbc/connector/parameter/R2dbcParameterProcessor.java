package io.graphoenix.r2dbc.connector.parameter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class R2dbcParameterProcessor {

    private final Jsonb jsonb;

    private final JsonProvider jsonProvider;

    @Inject
    public R2dbcParameterProcessor(Jsonb jsonb, JsonProvider jsonProvider) {
        this.jsonb = jsonb;
        this.jsonProvider = jsonProvider;
    }

    public Map<String, Object> process(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), processValue(v.getValue())), HashMap::putAll);
    }

    @SuppressWarnings("unchecked")
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
        } else if (value instanceof JsonValue) {
            return value.toString();
        } else if (value instanceof Map) {
            return jsonProvider.createObjectBuilder((Map<String, ?>) value).build().toString();
        } else if (value instanceof Collection) {
            return jsonProvider.createArrayBuilder((Collection<?>) value).build().toString();
        }
        return jsonb.toJson(value);
    }
}
