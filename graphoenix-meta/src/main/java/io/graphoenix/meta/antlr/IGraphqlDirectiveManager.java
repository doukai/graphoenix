package io.graphoenix.meta.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlDirectiveManager {
    Map<String, GraphqlParser.DirectiveDefinitionContext> register(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext);

    Optional<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinition(String directiveName);

    Stream<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinitions();
}
