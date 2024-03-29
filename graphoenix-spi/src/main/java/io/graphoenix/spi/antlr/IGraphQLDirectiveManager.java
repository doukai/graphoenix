package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLDirectiveManager {
    Map<String, GraphqlParser.DirectiveDefinitionContext> register(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext);

    Optional<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinition(String directiveName);

    Stream<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinitions();

    void clear();
}
