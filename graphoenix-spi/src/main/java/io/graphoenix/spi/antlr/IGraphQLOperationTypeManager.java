package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLOperationTypeManager {

    Map<String, GraphqlParser.OperationTypeDefinitionContext> register(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext);

    boolean isOperation(String operationTypeName);

    Optional<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinition(String operationTypeName);

    Stream<GraphqlParser.OperationTypeDefinitionContext> getOperationTypeDefinitions();

    void clear();
}
