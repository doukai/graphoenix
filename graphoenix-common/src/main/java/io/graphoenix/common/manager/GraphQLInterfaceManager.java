package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInterfaceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLInterfaceManager implements IGraphQLInterfaceManager {

    private final Map<String, GraphqlParser.InterfaceTypeDefinitionContext> interfaceTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.InterfaceTypeDefinitionContext> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        interfaceTypeDefinitionMap.put(interfaceTypeDefinitionContext.name().getText(), interfaceTypeDefinitionContext);
        return interfaceTypeDefinitionMap;
    }

    @Override
    public boolean isInterface(String interfaceTypeName) {
        return interfaceTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(interfaceTypeName));
    }

    @Override
    public Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinition(String interfaceTypeName) {
        return interfaceTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(interfaceTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinitions() {
        return interfaceTypeDefinitionMap.values().stream();
    }
}
