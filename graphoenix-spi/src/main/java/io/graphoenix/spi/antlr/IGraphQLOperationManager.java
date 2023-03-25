package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLOperationManager {

    Map<String, GraphqlParser.OperationDefinitionContext> register(GraphqlParser.OperationDefinitionContext operationDefinitionContext);

    Optional<GraphqlParser.OperationDefinitionContext> getOperationDefinition(String operationTypeName);

    Stream<GraphqlParser.OperationDefinitionContext> getOperationDefinitions();

    void clear();
}
