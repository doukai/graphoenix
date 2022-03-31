package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLObjectManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLObjectManager implements IGraphQLObjectManager {

    private final Map<String, GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.ObjectTypeDefinitionContext> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        objectTypeDefinitionMap.put(objectTypeDefinitionContext.name().getText(), objectTypeDefinitionContext);
        Logger.info("registered objectType {}", objectTypeDefinitionContext.name().getText());
        return objectTypeDefinitionMap;
    }

    @Override
    public boolean isObject(String objectTypeName) {
        return objectTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(objectTypeName));
    }

    @Override
    public Optional<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinition(String objectTypeName) {
        return objectTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinitions() {
        return objectTypeDefinitionMap.values().stream();
    }

    @Override
    public boolean isContainerType(String objectTypeName) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = objectTypeDefinitionMap.get(objectTypeName);
        return objectTypeDefinitionContext.directives() != null && objectTypeDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("containerType"));
    }

    @Override
    public boolean isNotContainerType(String objectTypeName) {
        return !isContainerType(objectTypeName);
    }

    @Override
    public void clear() {
        objectTypeDefinitionMap.clear();
        Logger.debug("clear all objectType");
    }
}
