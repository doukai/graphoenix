package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLEnumManager {

    Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext);

    boolean isEnum(String enumTypeName);

    Optional<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinition(String enumTypeName);

    Stream<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinitions();

    void clear();
}
