package io.graphoenix.common.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphqlDirectiveManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GraphqlDirectiveManager implements IGraphqlDirectiveManager {

    private final Map<String, GraphqlParser.DirectiveDefinitionContext> directiveDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.DirectiveDefinitionContext> register(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        directiveDefinitionMap.put(directiveDefinitionContext.name().getText(), directiveDefinitionContext);
        return directiveDefinitionMap;
    }

    @Override
    public Optional<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinition(String directiveName) {
        return directiveDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(directiveName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.DirectiveDefinitionContext> getDirectiveDefinitions() {
        return directiveDefinitionMap.values().stream();
    }
}
