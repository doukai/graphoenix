package io.graphoenix.spi.handler;

import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface OperationHandler {

    Mono<String> query(String graphQL, Map<String, String> variables);

    Flux<Tuple2<String, String>> querySelections(String graphQL, Map<String, String> variables);

    Mono<String> mutation(String graphQL, Map<String, String> variables);
}
