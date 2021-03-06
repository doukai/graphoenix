package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLUnionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphQLUnionManager implements IGraphQLUnionManager {

    private final Map<String, GraphqlParser.UnionTypeDefinitionContext> unionTypeDefinitionContextMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.UnionTypeDefinitionContext> register(GraphqlParser.UnionTypeDefinitionContext unionTypeDefinitionContext) {
        unionTypeDefinitionContextMap.put(unionTypeDefinitionContext.name().getText(), unionTypeDefinitionContext);
        return unionTypeDefinitionContextMap;
    }

    @Override
    public boolean isUnion(String unionTypeName) {
        return unionTypeDefinitionContextMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(unionTypeName));
    }

    @Override
    public Optional<GraphqlParser.UnionTypeDefinitionContext> getUnionTypeDefinition(String unionTypeName) {
        return unionTypeDefinitionContextMap.entrySet().stream().filter(entry -> entry.getKey().equals(unionTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.UnionTypeDefinitionContext> getUnionTypeDefinitions() {
        return unionTypeDefinitionContextMap.values().stream();
    }
}
