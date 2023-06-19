package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

public interface SubscriptionHandler {

    Mono<JsonValue> subscription(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<JsonValue> subscription(OperationHandler operationHandler, GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Mono<JsonValue> invoke(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue);
}
