package io.graphoenix.r2dbc.connector.parameter;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class R2dbcParameterProcessor {

    private final GsonBuilder jsonBuilder = new GsonBuilder();

    public Map<String, Object> process(Map<String, Object> parameters) {
        return parameters.entrySet().stream().collect(Collectors.toList())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> processValue(entry.getValue())));
    }

    private Object processValue(Object value) {
        if (value.getClass().isPrimitive()) {
            return value;
        } else if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }
        Type typeOfT = new TypeToken<List<String>>(){}.getType();
        List<String> o = jsonBuilder.create().fromJson(",", typeOfT);
        return jsonBuilder.create().toJson(value);
    }
}
