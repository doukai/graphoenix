package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDirectiveManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLDirectiveManager implements IGraphQLDirectiveManager {

    private final Map<String, GraphqlParser.DirectiveDefinitionContext> directiveDefinitionMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.DirectiveDefinitionContext> register(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        directiveDefinitionMap.put(directiveDefinitionContext.name().getText(), directiveDefinitionContext);
        Logger.info("registered directive {}", directiveDefinitionContext.name().getText());
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

    @Override
    public void clear() {
        directiveDefinitionMap.clear();
        Logger.debug("clear all directive");
    }
}
