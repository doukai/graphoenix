package io.graphoenix.core.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.util.Map;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.core.error.GraphQLErrorType.NON_NULL_VALUE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OPERATION_VARIABLE_NOT_EXIST;

@ApplicationScoped
public class GraphQLVariablesProcessor {

    private final IGraphQLDocumentManager manager;

    @Inject
    public GraphQLVariablesProcessor(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(String graphQL, Map<String, JsonElement> variables) {
        return buildVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonElement> variables) {
        if (operationDefinitionContext.variableDefinitions() != null) {
            operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> processSelection(selectionContext, operationDefinitionContext, variables));
        }
        return operationDefinitionContext;
    }

    private void processSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonElement> variables) {
        if (selectionContext.field() != null && selectionContext.field().arguments() != null) {
            selectionContext.field().arguments().argument().forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables));
        }
        if (selectionContext.field() != null && selectionContext.field().selectionSet() != null) {
            selectionContext.field().selectionSet().selection().forEach(subSelectionContext -> processSelection(subSelectionContext, operationDefinitionContext, variables));
        }
    }

    private void replaceVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonElement> variables) {
        if (valueWithVariableContext.variable() != null) {
            GraphqlParser.ValueWithVariableContext valueContext = getValueByVariable(valueWithVariableContext.variable(), operationDefinitionContext, variables);
            Logger.debug("replace variable {} to {}", valueWithVariableContext.getChild(valueWithVariableContext.getChildCount() - 1).getText(), valueContext.getText());
            valueWithVariableContext.removeLastChild();
            if (valueContext.BooleanValue() != null) {
                valueContext.BooleanValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.BooleanValue());
            } else if (valueContext.IntValue() != null) {
                valueContext.IntValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.IntValue());
            } else if (valueContext.FloatValue() != null) {
                valueContext.FloatValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.FloatValue());
            } else if (valueContext.StringValue() != null) {
                valueContext.StringValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.StringValue());
            } else if (valueContext.NullValue() != null) {
                valueContext.NullValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.NullValue());
            } else if (valueContext.enumValue() != null) {
                valueContext.enumValue().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.enumValue());
            } else if (valueContext.objectValueWithVariable() != null) {
                valueContext.objectValueWithVariable().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.objectValueWithVariable());
            } else if (valueContext.arrayValueWithVariable() != null) {
                valueContext.arrayValueWithVariable().setParent(valueWithVariableContext);
                valueWithVariableContext.addChild(valueContext.arrayValueWithVariable());
            }
        } else if (valueWithVariableContext.objectValueWithVariable() != null) {
            valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable()
                    .forEach(objectFieldWithVariableContext -> replaceVariable(objectFieldWithVariableContext.valueWithVariable(), operationDefinitionContext, variables));
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            valueWithVariableContext.arrayValueWithVariable().valueWithVariable()
                    .forEach(subValueWithVariableContext -> replaceVariable(subValueWithVariableContext, operationDefinitionContext, variables));
        }
    }

    private GraphqlParser.ValueWithVariableContext getValueByVariable(GraphqlParser.VariableContext variableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonElement> variables) {
        return operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .filter(variableDefinitionContext -> variableDefinitionContext.variable().name().getText().equals(variableContext.name().getText()))
                .map(variableDefinitionContext -> variableToValue(variableDefinitionContext, variables.get(variableDefinitionContext.variable().name().getText())))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(OPERATION_VARIABLE_NOT_EXIST.bind(variableContext.name().getText(), operationDefinitionContext.name().getText())));
    }

    private GraphqlParser.ValueWithVariableContext variableToValue(GraphqlParser.VariableDefinitionContext variableDefinitionContext, JsonElement variable) {
        if (variable == null) {
            if (variableDefinitionContext.type().nonNullType() != null) {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(variableDefinitionContext.variable().name()));
            } else {
                variable = JsonNull.INSTANCE;
            }
        }
        return DOCUMENT_UTIL.getGraphqlParser(variable.toString()).valueWithVariable();
    }
}
