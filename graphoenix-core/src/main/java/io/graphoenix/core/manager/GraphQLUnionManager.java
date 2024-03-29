package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLUnionManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLUnionManager implements IGraphQLUnionManager {

    private final Map<String, GraphqlParser.UnionTypeDefinitionContext> unionTypeDefinitionContextMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.UnionTypeDefinitionContext> register(GraphqlParser.UnionTypeDefinitionContext unionTypeDefinitionContext) {
        unionTypeDefinitionContextMap.put(unionTypeDefinitionContext.name().getText(), unionTypeDefinitionContext);
        Logger.info("registered unionType {}", unionTypeDefinitionContext.name().getText());
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

    @Override
    public void clear() {
        unionTypeDefinitionContextMap.clear();
        Logger.debug("clear all unionType");
    }
}
