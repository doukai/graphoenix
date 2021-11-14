package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLScalarManager {

    Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext);

    boolean isScalar(String scalarTypeName);

    Optional<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinition(String scalarTypeName);

    Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinitions();
}
