package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.Map;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.error.GraphQLErrorType.NON_NULL_VALUE_NOT_EXIST;
import static io.graphoenix.spi.error.GraphQLErrorType.OPERATION_VARIABLE_NOT_EXIST;

@ApplicationScoped
public class GraphQLVariablesProcessor {

    public GraphqlParser.OperationDefinitionContext buildVariables(String graphQL, Map<String, String> variables) {
        return buildVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (operationDefinitionContext.variableDefinitions() != null) {
            operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> processSelection(selectionContext, operationDefinitionContext, variables));
        }
        return operationDefinitionContext;
    }

    private void processSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (selectionContext.field() != null && selectionContext.field().arguments() != null) {
            selectionContext.field().arguments().argument().forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables));
        }
        if (selectionContext.field() != null && selectionContext.field().selectionSet() != null) {
            selectionContext.field().selectionSet().selection().forEach(subSelectionContext -> processSelection(subSelectionContext, operationDefinitionContext, variables));
        }
    }

    private void replaceVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (valueWithVariableContext.variable() != null) {
            GraphqlParser.ValueContext valueContext = getValueByVariable(valueWithVariableContext.variable(), operationDefinitionContext, variables);
            Logger.debug("replace variable {} to {}", valueWithVariableContext.getChild(valueWithVariableContext.getChildCount() - 1).getText(), valueContext.getText());
            valueWithVariableContext.removeLastChild();
            valueWithVariableContext.addChild(valueContext);
        } else if (valueWithVariableContext.objectValueWithVariable() != null) {
            valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable()
                    .forEach(objectFieldWithVariableContext -> replaceVariable(objectFieldWithVariableContext.valueWithVariable(), operationDefinitionContext, variables));
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            valueWithVariableContext.arrayValueWithVariable().valueWithVariable()
                    .forEach(subValueWithVariableContext -> replaceVariable(subValueWithVariableContext, operationDefinitionContext, variables));
        }
    }

    private GraphqlParser.ValueContext getValueByVariable(GraphqlParser.VariableContext variableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        return operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .filter(variableDefinitionContext -> variableDefinitionContext.variable().name().getText().equals(variableContext.name().getText()))
                .map(variableDefinitionContext -> variableToValue(variableDefinitionContext, variables.get(variableDefinitionContext.variable().name().getText())))
                .findFirst()
                .orElseThrow(() -> new GraphQLProblem(OPERATION_VARIABLE_NOT_EXIST.bind(variableContext.name().getText(), operationDefinitionContext.name().getText())));
    }

    private GraphqlParser.ValueContext variableToValue(GraphqlParser.VariableDefinitionContext variableDefinitionContext, String variable) {
        if (variable == null && variableDefinitionContext.type().nonNullType() != null) {
            throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(variableDefinitionContext.variable().name()));
        }
        return DOCUMENT_UTIL.getGraphqlParser(variable).value();
    }
}
