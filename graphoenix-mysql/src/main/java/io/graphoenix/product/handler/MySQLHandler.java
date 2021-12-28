package io.graphoenix.product.handler;

import io.graphoenix.spi.handler.IOperationHandler;
import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MySQLHandler extends IOperationHandler {

    Mono<String> query(String graphQL, Map<String, Object> parameters);

    Flux<Tuple2<String, String>> querySelections(String graphQL, Map<String, Object> parameters);

    Mono<String> mutation(String graphQL, Map<String, Object> parameters);
}
