package io.graphoenix.spi.handler;

import reactor.core.publisher.Mono;

public interface FetchHandler {

    Mono<String> operation(String packageName, String graphql);
}
