package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import io.vavr.Tuple2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OperationHandler {

    Mono<String> query(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Flux<Tuple2<String, String>> querySelections(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<String> mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext);
}
