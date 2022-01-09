package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

public class GraphQLVariablesProcessor {

    Map<String, Object> process(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, Object> variables) {
        operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .map(variableDefinitionContext -> {
                    Object variable = variables.get(variableDefinitionContext.variable().name().getText());
                    return variableToObject(variableDefinitionContext.type(), variable);
                });
    }

    Object variableToObject(GraphqlParser.TypeContext typeContext, Object object) {


    }
}
