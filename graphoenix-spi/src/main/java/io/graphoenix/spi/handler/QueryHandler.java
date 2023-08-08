package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

public interface QueryHandler {

    Mono<JsonValue> query(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<JsonValue> query(OperationHandler operationHandler, GraphqlParser.OperationDefinitionContext operationDefinitionContext);
}
