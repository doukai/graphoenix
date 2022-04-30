package io.graphoenix.spi.handler;

import com.google.gson.JsonElement;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MutationHandler {

    Mono<JsonElement> mutation(String graphQL, Map<String, JsonElement> variables);
}
