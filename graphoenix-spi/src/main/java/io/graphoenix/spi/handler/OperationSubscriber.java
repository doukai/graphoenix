package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OperationSubscriber {

    Flux<JsonValue> subscriptionOperation(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Mono<JsonValue> jsonValueMono);

    Mono<Void> sendMutation(String typeName, JsonValue jsonValue);
}
