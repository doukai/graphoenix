package io.graphoenix.spi.handler;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

public interface QueryHandler {

    BiFunction<String, Map<String, String>, Mono<String>> getInvokeMethod(String name);
}
