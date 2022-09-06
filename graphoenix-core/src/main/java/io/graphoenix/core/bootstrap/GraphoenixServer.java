package io.graphoenix.core.bootstrap;

import reactor.core.publisher.Mono;

public interface GraphoenixServer {

    Mono<Void> run();
}
