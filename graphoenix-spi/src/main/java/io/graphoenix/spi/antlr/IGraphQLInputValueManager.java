package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLInputValueManager {

    Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    Stream<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName);

    Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName, String inputValueName);
}
