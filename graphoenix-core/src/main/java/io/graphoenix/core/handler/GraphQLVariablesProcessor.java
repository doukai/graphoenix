package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonValue;
import org.tinylog.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.NON_NULL_VALUE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OPERATION_VARIABLE_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.NULL;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.json.JsonValue.ValueType.TRUE;

@ApplicationScoped
public class GraphQLVariablesProcessor {

    public GraphqlParser.OperationDefinitionContext buildVariables(String graphQL, Map<String, JsonValue> variables) {
        return buildVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        if (operationDefinitionContext.variableDefinitions() != null) {
            operationDefinitionContext.selectionSet().selection().forEach(selectionContext -> processSelection(selectionContext, operationDefinitionContext, variables));
        }
        return operationDefinitionContext;
    }

    private void processSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        if (selectionContext.field() != null) {
            if (selectionContext.field().arguments() != null) {
                selectionContext.field().arguments().argument().forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables, skipNullArguments(selectionContext.field(), variables)));
            }
            if (selectionContext.field().selectionSet() != null) {
                selectionContext.field().selectionSet().selection().forEach(subSelectionContext -> processSelection(subSelectionContext, operationDefinitionContext, variables));
            }
        }
    }

    private void replaceVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables, boolean skipNullArguments) {
        if (valueWithVariableContext.variable() != null) {
            GraphqlParser.ValueWithVariableContext valueContext = getValueByVariable(valueWithVariableContext.variable(), operationDefinitionContext, variables);
            if (skipNullArguments && valueContext.NullValue() != null) {
                valueWithVariableContext.getParent().removeLastChild();
                valueWithVariableContext.getParent().removeLastChild();
                valueWithVariableContext.getParent().removeLastChild();
                return;
            }
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
                    .forEach(objectFieldWithVariableContext -> replaceVariable(objectFieldWithVariableContext.valueWithVariable(), operationDefinitionContext, variables, skipNullArguments));
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            valueWithVariableContext.arrayValueWithVariable().valueWithVariable()
                    .forEach(subValueWithVariableContext -> replaceVariable(subValueWithVariableContext, operationDefinitionContext, variables, skipNullArguments));
        }
    }

    private GraphqlParser.ValueWithVariableContext getValueByVariable(GraphqlParser.VariableContext variableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        return operationDefinitionContext.variableDefinitions().variableDefinition().stream()
                .filter(variableDefinitionContext -> variableDefinitionContext.variable().name().getText().equals(variableContext.name().getText()))
                .map(variableDefinitionContext -> variableToValue(variableDefinitionContext, variables.get(variableDefinitionContext.variable().name().getText())))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(OPERATION_VARIABLE_NOT_EXIST.bind(variableContext.name().getText(), operationDefinitionContext.name().getText())));
    }

    private GraphqlParser.ValueWithVariableContext variableToValue(GraphqlParser.VariableDefinitionContext variableDefinitionContext, JsonValue variable) {
        if (variable == null) {
            if (variableDefinitionContext.type().nonNullType() != null) {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(variableDefinitionContext.variable().name().getText()));
            } else {
                variable = JsonValue.NULL;
            }
        } else {
            if (variable.getValueType().equals(NULL)) {
                if (variableDefinitionContext.type().nonNullType() != null) {
                    throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(variableDefinitionContext.variable().name().getText()));
                }
            }
        }
        return DOCUMENT_UTIL.getGraphqlParser(jsonElementToVariableString(variable)).valueWithVariable();
    }

    public String jsonElementToVariableString(JsonValue element) {
        if (element.getValueType().equals(OBJECT)) {
            return "{"
                    .concat(
                            element.asJsonObject().entrySet().stream()
                                    .map(entry -> entry.getKey().concat(": ").concat(jsonElementToVariableString(entry.getValue())))
                                    .collect(Collectors.joining(" "))
                    )
                    .concat("}");
        } else if (element.getValueType().equals(ARRAY)) {
            return "["
                    .concat(
                            element.asJsonArray().stream().map(this::jsonElementToVariableString).collect(Collectors.joining(", "))
                    )
                    .concat("]");
        } else {
            return element.toString();
        }
    }

    private Optional<GraphqlParser.DirectiveContext> getSkipNullArguments(GraphqlParser.FieldContext fieldContext) {
        return Stream.ofNullable(fieldContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals("skipNullArguments"))
                .findFirst();
    }

    private boolean skipNullArguments(GraphqlParser.FieldContext fieldContext, Map<String, JsonValue> variables) {
        return getSkipNullArguments(fieldContext).stream()
                .flatMap(directiveContext -> Stream.ofNullable(directiveContext.arguments()))
                .flatMap(argumentsContext -> argumentsContext.argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("if"))
                .findFirst()
                .map(argumentContext -> {
                            if (argumentContext.valueWithVariable().variable() != null) {
                                JsonValue jsonValue = variables.get(argumentContext.valueWithVariable().variable().name().getText());
                                return jsonValue != null && jsonValue.getValueType().equals(TRUE);
                            } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
                                return Boolean.valueOf(argumentContext.valueWithVariable().BooleanValue().getText());
                            } else {
                                return false;
                            }
                        }
                )
                .orElse(false);
    }
}
