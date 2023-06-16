package io.graphoenix.spi.handler;

import jakarta.json.JsonValue;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface SubscriptionHandler {

    Flux<JsonValue> subscription(String graphQL, Map<String, JsonValue> variables);

    Flux<JsonValue> subscription(OperationHandler operationHandler, String graphQL, Map<String, JsonValue> variables);
}
