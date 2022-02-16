package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInputValueManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLInputValueManager implements IGraphQLInputValueManager {

    private final Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> inputValueDefinitionTree = new HashMap<>();

    @Override
    public Map<String, Map<String, GraphqlParser.InputValueDefinitionContext>> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        Map<String, GraphqlParser.InputValueDefinitionContext> inputValueMap = new HashMap<>();
        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition()
                .forEach(inputValueDefinitionContext -> {
                            inputValueMap.put(inputValueDefinitionContext.name().getText(), inputValueDefinitionContext);
                            Logger.info("registered inputObject {} inputValue {}", inputObjectTypeDefinitionContext.name().getText(), inputValueDefinitionContext.name().getText());
                        }
                );
        inputValueDefinitionTree.put(inputObjectTypeDefinitionContext.name().getText(), inputValueMap);
        return inputValueDefinitionTree;
    }

    @Override
    public Stream<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName) {
        return inputValueDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName)).map(Map.Entry::getValue)
                .flatMap(entry -> entry.values().stream());
    }

    @Override
    public Optional<GraphqlParser.InputValueDefinitionContext> getInputValueDefinitions(String inputObjectTypeName, String inputValueName) {
        return inputValueDefinitionTree.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectTypeName))
                .map(Map.Entry::getValue).findFirst()
                .flatMap(inputValueDefinitionMap ->
                        inputValueDefinitionMap.entrySet().stream()
                                .filter(entry -> entry.getKey().equals(inputValueName))
                                .map(Map.Entry::getValue)
                                .findFirst()
                );
    }

    @Override
    public void clear() {
        inputValueDefinitionTree.clear();
        Logger.debug("clear all inputValue");
    }
}
