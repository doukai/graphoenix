package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLInputObjectManager {
    Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext);

    boolean isInputObject(String inputObjectName);

    Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinition(String inputObjectName);

    Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinitions();
}
