package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphQLInterfaceManager {

    Map<String, GraphqlParser.InterfaceTypeDefinitionContext> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Map<String, Map<String, GraphqlParser.ObjectTypeDefinitionContext>> registerImplements(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean isInterface(String interfaceTypeName);

    Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinition(String interfaceTypeName);

    Stream<GraphqlParser.ObjectTypeDefinitionContext> getImplementsObjectTypeDefinition(String interfaceTypeName);

    Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinitions();

    void clear();
}
