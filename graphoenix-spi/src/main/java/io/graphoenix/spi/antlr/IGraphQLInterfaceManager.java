package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLInterfaceManager {

    Map<String, GraphqlParser.InterfaceTypeDefinitionContext> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    boolean isInterface(String interfaceTypeName);

    Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinition(String interfaceTypeName);

    Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinitions();
}
