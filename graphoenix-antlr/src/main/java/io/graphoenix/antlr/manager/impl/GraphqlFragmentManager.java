package io.graphoenix.antlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.IGraphqlFragmentManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GraphqlFragmentManager implements IGraphqlFragmentManager {

    private final Map<String, Map<String, GraphqlParser.FragmentDefinitionContext>> fragmentDefinitionTree = new ConcurrentHashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.FragmentDefinitionContext>> register(GraphqlParser.FragmentDefinitionContext fragmentDefinitionContext) {
        Map<String, GraphqlParser.FragmentDefinitionContext> fragmentDefinition = fragmentDefinitionTree.get(fragmentDefinitionContext.typeCondition().typeName().getText());
        if (fragmentDefinition == null) {
            fragmentDefinition = new ConcurrentHashMap<>();
        }
        fragmentDefinition.put(fragmentDefinitionContext.fragmentName().getText(), fragmentDefinitionContext);
        fragmentDefinitionTree.put(fragmentDefinitionContext.typeCondition().typeName().getText(),
                fragmentDefinition);

        return fragmentDefinitionTree;
    }

    @Override
    public Stream<GraphqlParser.FragmentDefinitionContext> getFragmentDefinitions(String objectTypeName) {
        return fragmentDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue)
                .flatMap(fragmentDefinitionContextMap -> fragmentDefinitionContextMap.values().stream());
    }

    @Override
    public Optional<GraphqlParser.FragmentDefinitionContext> getFragmentDefinition(String objectTypeName, String fragmentName) {
        return fragmentDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(objectTypeName))
                .map(Map.Entry::getValue).findFirst()
                .flatMap(fragmentDefinitionContextMap -> fragmentDefinitionContextMap.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(fragmentName))
                        .map(Map.Entry::getValue).findFirst());
    }
}
