package io.graphoenix.core.schema;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class JsonSchemaManager {

    private final Map<String, String> jsonSchemaMap = new LinkedHashMap<>();

    public String getJsonSchema(String objectName) {
        String jsonSchema = jsonSchemaMap.get(objectName);
        if (jsonSchema == null) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("META-INF/schema/".concat(objectName).concat(".json"));
            String schema = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            jsonSchemaMap.put(objectName, schema);
            return schema;
        }
        return jsonSchema;
    }
}
