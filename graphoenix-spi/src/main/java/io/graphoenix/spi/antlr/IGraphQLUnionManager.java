package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLUnionManager {

    Map<String, GraphqlParser.UnionTypeDefinitionContext> register(GraphqlParser.UnionTypeDefinitionContext unionTypeDefinitionContext);

    boolean isUnion(String unionTypeName);

    Optional<GraphqlParser.UnionTypeDefinitionContext> getUnionTypeDefinition(String unionTypeName);

    Stream<GraphqlParser.UnionTypeDefinitionContext> getUnionTypeDefinitions();
}
