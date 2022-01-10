package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.Map;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class GraphQLVariablesProcessor {

    public String buildVariables(String graphQL, Map<String, String> variables) {
        GraphqlParser.OperationDefinitionContext operationDefinitionContext = DOCUMENT_UTIL.graphqlToOperation(graphQL);
        if (operationDefinitionContext.variableDefinitions() != null) {
            operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> processSelection(selectionContext, operationDefinitionContext, variables));
            operationDefinitionContext.variableDefinitions().removeLastChild();
        }
        return operationDefinitionContext.getText();
    }

    private void processSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (selectionContext.field() != null && selectionContext.field().arguments() != null) {
            selectionContext.field().arguments().argument().stream()
                    .filter(argumentContext -> argumentContext.valueWithVariable().variable() != null)
                    .forEach(argumentContext -> {
                                GraphqlParser.ValueContext valueContext = getValueByVariable(argumentContext.valueWithVariable().variable(), operationDefinitionContext, variables);
                                argumentContext.removeLastChild();
                                argumentContext.addChild(valueContext);
                            }
                    );
        }
        if (selectionContext.field() != null && selectionContext.field().selectionSet() != null) {
            selectionContext.field().selectionSet().selection().forEach(subSelectionContext -> processSelection(subSelectionContext, operationDefinitionContext, variables));
        }
    }

    private GraphqlParser.ValueContext getValueByVariable(GraphqlParser.VariableContext variableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        return operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .filter(variableDefinitionContext -> variableDefinitionContext.variable().name().getText().equals(variableContext.name().getText()))
                .map(variableDefinitionContext -> variableToValue(variableDefinitionContext.type(), variables.get(variableDefinitionContext.variable().name().getText())))
                .findFirst()
                .orElseThrow();
    }

    private GraphqlParser.ValueContext variableToValue(GraphqlParser.TypeContext typeContext, String variable) {
        if (variable == null && typeContext.nonNullType() != null) {
            //TODO
        }
        return DOCUMENT_UTIL.getGraphqlParser(variable).value();
    }
}
