package io.graphonix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlScalarManager {

    Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext);

    boolean isScalar(String scalarTypeName);

    Optional<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinition(String scalarTypeName);

    Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinitions();
}
