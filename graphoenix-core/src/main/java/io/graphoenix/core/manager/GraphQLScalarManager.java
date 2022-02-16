package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLScalarManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLScalarManager implements IGraphQLScalarManager {

    private final Map<String, GraphqlParser.ScalarTypeDefinitionContext> scalarTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        scalarTypeDefinitionMap.put(scalarTypeDefinitionContext.name().getText(), scalarTypeDefinitionContext);
        Logger.info("registered scalarType {}", scalarTypeDefinitionContext.name().getText());
        return scalarTypeDefinitionMap;
    }

    @Override
    public boolean isScalar(String scalarTypeName) {
        return scalarTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(scalarTypeName));
    }

    @Override
    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinition(String scalarTypeName) {
        return scalarTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(scalarTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinitions() {
        return scalarTypeDefinitionMap.values().stream();
    }

    @Override
    public void clear() {
        scalarTypeDefinitionMap.clear();
        Logger.debug("clear all scalarType");
    }
}
