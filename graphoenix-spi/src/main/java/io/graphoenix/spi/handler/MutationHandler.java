package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

public interface MutationHandler {

    Mono<JsonValue> mutation(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<JsonValue> mutation(OperationHandler operationHandler, GraphqlParser.OperationDefinitionContext operationDefinitionContext);
}
