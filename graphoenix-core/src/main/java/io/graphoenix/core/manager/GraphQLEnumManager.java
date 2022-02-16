package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLEnumManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLEnumManager implements IGraphQLEnumManager {

    private final Map<String, GraphqlParser.EnumTypeDefinitionContext> enumTypeDefinitionMap = new HashMap<>();

    @Override
    public Map<String, GraphqlParser.EnumTypeDefinitionContext> register(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        enumTypeDefinitionMap.put(enumTypeDefinitionContext.name().getText(), enumTypeDefinitionContext);
        Logger.info("registered enum {}", enumTypeDefinitionContext.name().getText());
        return enumTypeDefinitionMap;
    }

    @Override
    public boolean isEnum(String enumTypeName) {
        return enumTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(enumTypeName));
    }

    @Override
    public Optional<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinition(String enumTypeName) {
        return enumTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(enumTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.EnumTypeDefinitionContext> getEnumTypeDefinitions() {
        return enumTypeDefinitionMap.values().stream();
    }

    @Override
    public void clear() {
        enumTypeDefinitionMap.clear();
        Logger.debug("clear all enum");
    }
}
