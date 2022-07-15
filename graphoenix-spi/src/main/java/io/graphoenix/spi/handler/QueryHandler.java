package io.graphoenix.spi.handler;

import com.google.gson.JsonElement;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import java.util.Map;

public interface QueryHandler {

    PublisherBuilder<JsonValue> query(String graphQL, Map<String, JsonElement> variables);
}
