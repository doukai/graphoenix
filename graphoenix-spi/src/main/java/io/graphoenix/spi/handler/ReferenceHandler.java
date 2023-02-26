package io.graphoenix.spi.handler;

import reactor.core.publisher.Mono;

public interface ReferenceHandler {

    Mono<String> operation(String graphql);
}
