package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;
import reactor.core.publisher.Mono;

public interface SubscriptionDataListener {
    Mono<JsonValue> merge(GraphqlParser.OperationDefinitionContext operationDefinitionContext, JsonValue jsonValue);
}
