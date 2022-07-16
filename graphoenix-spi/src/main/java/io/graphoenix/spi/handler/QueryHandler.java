package io.graphoenix.spi.handler;

import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface QueryHandler {

    Mono<JsonValue> query(String graphQL, Map<String, JsonValue> variables);
}
