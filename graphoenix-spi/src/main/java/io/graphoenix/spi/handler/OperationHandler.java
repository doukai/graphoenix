package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import reactor.core.publisher.Mono;

public interface OperationHandler {

    Mono<String> query(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<String> mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext);
}
