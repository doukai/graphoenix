package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInputObjectManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

@ApplicationScoped
public class GraphQLInputObjectManager implements IGraphQLInputObjectManager {

    private final Map<String, GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionMap = new LinkedHashMap<>();

    private final Map<String, Map<String, GraphqlParser.InputObjectTypeDefinitionContext>> implementsMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.InputObjectTypeDefinitionContext> register(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        inputObjectTypeDefinitionMap.put(inputObjectTypeDefinitionContext.name().getText(), inputObjectTypeDefinitionContext);
        Logger.info("registered inputObject {}", inputObjectTypeDefinitionContext.name().getText());

        return inputObjectTypeDefinitionMap;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.InputObjectTypeDefinitionContext>> registerImplements(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        getInterfaceNames(inputObjectTypeDefinitionContext.directives())
                .forEach(interfaceName -> {
                            implementsMap.computeIfAbsent(interfaceName, key -> new LinkedHashMap<>());
                            implementsMap.get(interfaceName).put(inputObjectTypeDefinitionContext.name().getText(), inputObjectTypeDefinitionContext);
                        }
                );
        return implementsMap;
    }

    @Override
    public boolean isInputObject(String inputObjectName) {
        return inputObjectTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(inputObjectName));
    }

    @Override
    public Optional<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinition(String inputObjectName) {
        return inputObjectTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(inputObjectName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getImplementsInputObjectTypeDefinition(String inputObjectName) {
        return Stream.ofNullable(implementsMap.get(inputObjectName)).flatMap(map -> map.values().stream());
    }

    @Override
    public Stream<GraphqlParser.InputObjectTypeDefinitionContext> getInputObjectTypeDefinitions() {
        return inputObjectTypeDefinitionMap.values().stream();
    }

    @Override
    public Stream<String> getInterfaceNames(GraphqlParser.DirectivesContext directivesContext) {
        if (directivesContext == null) {
            return Stream.empty();
        }
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("implementInputs"))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream().filter(argumentContext -> argumentContext.name().getText().equals("inputs")))
                .flatMap(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream())
                .map(valueWithVariableContext -> DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue()));
    }

    @Override
    public void clear() {
        inputObjectTypeDefinitionMap.clear();
        Logger.debug("clear all inputObject");
    }
}
