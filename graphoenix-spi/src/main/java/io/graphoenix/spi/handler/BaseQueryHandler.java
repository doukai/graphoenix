package io.graphoenix.spi.handler;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class BaseQueryHandler implements QueryHandler {

    private Map<String, BiFunction<String, Map<String, String>, Mono<String>>> invokeFunctions;

    @Override
    public BiFunction<String, Map<String, String>, Mono<String>> getInvokeMethod(String name) {
        return invokeFunctions.get(name);
    }

    protected void put(String name, BiFunction<String, Map<String, String>, Mono<String>> biFunction) {
        invokeFunctions.put(name, biFunction);
    }
}
