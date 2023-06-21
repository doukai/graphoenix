package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;

import java.util.Map;
import java.util.stream.Stream;

public interface SubscriptionDataGroupListener {

    SubscriptionDataGroupListener indexFilter(GraphqlParser.OperationDefinitionContext operationDefinitionContext, String operationId);

    Stream<Map.Entry<String, GraphqlParser.OperationDefinitionContext>> merged(JsonValue jsonValue);
}
