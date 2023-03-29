package io.graphoenix.spi.handler;

import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MutationHandler {

    Mono<JsonValue> mutation(String graphQL, Map<String, JsonValue> variables);

    Mono<JsonValue> mutation(String handlerName, String graphQL, Map<String, JsonValue> variables);
}
