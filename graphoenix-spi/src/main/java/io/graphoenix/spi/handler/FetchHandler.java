package io.graphoenix.spi.handler;

import reactor.core.publisher.Mono;

public interface FetchHandler {

    Mono<String> request(String packageName, String graphql);
}
