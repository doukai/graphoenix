package io.graphoenix.spi.handler;

import graphql.parser.antlr.GraphqlParser;
import jakarta.json.JsonValue;

public interface SubscriptionDataListener {

    SubscriptionDataListener indexFilter(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    boolean merged(JsonValue jsonValue);
}
