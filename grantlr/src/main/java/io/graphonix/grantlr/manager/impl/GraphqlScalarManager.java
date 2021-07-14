package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.IGraphqlScalarManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphqlScalarManager implements IGraphqlScalarManager {

    private final Map<String, GraphqlParser.ScalarTypeDefinitionContext> scalarTypeDefinitionMap = new HashMap<String, GraphqlParser.ScalarTypeDefinitionContext>() {{
        put("Int", null);
        put("Float", null);
        put("String", null);
        put("Boolean", null);
        put("ID", null);
    }};

    @Override
    public Map<String, GraphqlParser.ScalarTypeDefinitionContext> register(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        scalarTypeDefinitionMap.put(scalarTypeDefinitionContext.name().getText(), scalarTypeDefinitionContext);
        return scalarTypeDefinitionMap;
    }

    @Override
    public Optional<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinition(String scalarTypeName) {
        return scalarTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(scalarTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public boolean isScalar(String scalarTypeName) {
        return scalarTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(scalarTypeName));
    }

    @Override
    public Stream<GraphqlParser.ScalarTypeDefinitionContext> getScalarTypeDefinitions() {
        return scalarTypeDefinitionMap.values().stream();
    }
}
