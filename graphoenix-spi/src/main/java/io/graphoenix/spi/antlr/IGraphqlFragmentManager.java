package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlFragmentManager {

    Map<String, Map<String, GraphqlParser.FragmentDefinitionContext>> register(GraphqlParser.FragmentDefinitionContext fragmentDefinitionContext);

    Stream<GraphqlParser.FragmentDefinitionContext> getFragmentDefinitions(String objectTypeName);

    Optional<GraphqlParser.FragmentDefinitionContext> getFragmentDefinition(String objectTypeName, String fragmentName);
}
