package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.INVOKE_DIRECTIVES;

public class GraphQLVariablesProcessor {

    private final Set<String> invokeQuerySelectionNames;

    private final Set<String> invokeMutationSelectionNames;

    @Inject
    public GraphQLVariablesProcessor(IGraphQLDocumentManager manager) {
        invokeQuerySelectionNames = manager.getFields(manager.getQueryOperationTypeName().orElseThrow())
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(excludeDirectiveName -> excludeDirectiveName.equals(directiveContext.name().getText())))
                )
                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                .collect(Collectors.toSet());
        invokeMutationSelectionNames = manager.getFields(manager.getMutationOperationTypeName().orElseThrow())
                .filter(fieldDefinitionContext ->
                        fieldDefinitionContext.directives().directive().stream()
                                .anyMatch(directiveContext ->
                                        Arrays.stream(INVOKE_DIRECTIVES)
                                                .anyMatch(excludeDirectiveName -> excludeDirectiveName.equals(directiveContext.name().getText())))
                )
                .map(fieldDefinitionContext -> fieldDefinitionContext.name().getText())
                .collect(Collectors.toSet());
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(String graphQL, Map<String, String> variables) {
        return buildVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (operationDefinitionContext.variableDefinitions() != null) {
            operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> processSelection(selectionContext, operationDefinitionContext, variables));
        }
        return operationDefinitionContext;
    }

    public GraphqlParser.OperationDefinitionContext buildInvokeVariables(String graphQL, Map<String, String> variables) {
        return buildInvokeVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildInvokeVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, String> variables) {
        if (operationDefinitionContext.variableDefinitions() != null) {
            if (operationDefinitionContext.operationType().QUERY() != null) {
                operationDefinitionContext.selectionSet().selection().stream()
                        .filter(selectionContext -> invokeQuerySelectionNames.contains(selectionContext.field().name().getText()))
                        .forEach(selectionContext -> selectionContext.field().arguments().argument()
                                .forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables))
                        );
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                operationDefinitionContext.selectionSet().selection().stream()
                        .filter(selectionContext -> invokeMutationSelectionNames.contains(selectionContext.field().name().getText()))
                        .forEach(selectionContext -> selectionContext.field().arguments().argument()
                                .forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables))
                        );
            }
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
                .map(variableDefinitionContext -> variableToValue(variableDefinitionContext.type(), variables.get(variableDefinitionContext.variable().name().getText())))
                .findFirst()
                .orElseThrow();
    }

    private GraphqlParser.ValueContext variableToValue(GraphqlParser.TypeContext typeContext, String variable) {
        if (variable == null && typeContext.nonNullType() != null) {
            throw new RuntimeException();
        }
        return DOCUMENT_UTIL.getGraphqlParser(variable).value();
    }
}
