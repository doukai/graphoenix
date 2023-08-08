package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;

import java.util.Map;

@ApplicationScoped
public class OperationPreprocessor {

    private final IGraphQLDocumentManager manager;
    private final GraphQLVariablesProcessor variablesProcessor;

    @Inject
    public OperationPreprocessor(IGraphQLDocumentManager manager, GraphQLVariablesProcessor variablesProcessor) {
        this.manager = manager;
        this.variablesProcessor = variablesProcessor;
    }

    public GraphqlParser.OperationDefinitionContext preprocess(String graphQL, Map<String, JsonValue> variables) {
        manager.registerFragment(graphQL);
        return variablesProcessor.buildVariables(graphQL, variables);
    }
}
