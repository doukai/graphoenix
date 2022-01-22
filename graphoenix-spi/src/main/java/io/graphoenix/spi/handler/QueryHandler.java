package io.graphoenix.spi.handler;

import com.google.gson.JsonElement;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface QueryHandler {

    Mono<JsonElement> query(String graphQL, Map<String, String> variables);
}
