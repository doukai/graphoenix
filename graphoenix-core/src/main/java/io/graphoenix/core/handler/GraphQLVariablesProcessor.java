package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.tinylog.Logger;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.NULL;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.json.JsonValue.ValueType.TRUE;

@ApplicationScoped
public class GraphQLVariablesProcessor {

    private final IGraphQLDocumentManager manager;
    private final JsonProvider jsonProvider;

    @Inject
    public GraphQLVariablesProcessor(IGraphQLDocumentManager manager, JsonProvider jsonProvider) {
        this.manager = manager;
        this.jsonProvider = jsonProvider;
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(String graphQL, Map<String, JsonValue> variables) {
        return buildVariables(DOCUMENT_UTIL.graphqlToOperation(graphQL), variables);
    }

    public GraphqlParser.OperationDefinitionContext buildVariables(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext;
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            objectTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(manager::getObject).orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
        } else if (operationDefinitionContext.operationType().MUTATION() != null) {
            objectTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(manager::getObject).orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
        } else {
            throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE.bind(operationDefinitionContext.operationType().getText()));
        }
        if (operationDefinitionContext.variableDefinitions() != null) {
            Map<String, JsonValue> processedDefaultValue = processDefaultValue(operationDefinitionContext, variables);
            operationDefinitionContext.selectionSet().selection()
                    .forEach(selectionContext -> {
                                GraphqlParser.FieldDefinitionContext fieldDefinitionContext = objectTypeDefinitionContext
                                        .fieldsDefinition().fieldDefinition().stream()
                                        .filter(subFieldDefinitionContext -> subFieldDefinitionContext.name().getText().equals(selectionContext.field().name().getText()))
                                        .findFirst()
                                        .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectTypeDefinitionContext.name().getText(), selectionContext.field().name().getText())));
                                processSelection(
                                        fieldDefinitionContext,
                                        selectionContext,
                                        operationDefinitionContext,
                                        processedDefaultValue
                                );
                                processFetchSelection(fieldDefinitionContext, selectionContext.field());
                                if (operationDefinitionContext.operationType().MUTATION() != null) {
                                    processFetchArgument(fieldDefinitionContext, selectionContext.field());
                                }
                            }
                    );
            if (operationDefinitionContext.directives() != null) {
                operationDefinitionContext.directives().directive().forEach(directiveContext -> processDirective(directiveContext, operationDefinitionContext, processedDefaultValue));
            }
        }
        return operationDefinitionContext;
    }

    private Map<String, JsonValue> processDefaultValue(GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        JsonObject defaultValueObject = Stream.ofNullable(operationDefinitionContext.variableDefinitions())
                .flatMap(variableDefinitionsContext -> variableDefinitionsContext.variableDefinition().stream())
                .filter(variableDefinitionContext -> variableDefinitionContext.defaultValue() != null && variables.get(variableDefinitionContext.variable().name().getText()) == null)
                .map(variableDefinitionContext -> new AbstractMap.SimpleEntry<>(variableDefinitionContext.variable().name().getText(), valueToJsonValue(variableDefinitionContext.defaultValue().value())))
                .collect(JsonCollectors.toJsonObject());

        if (!defaultValueObject.isEmpty()) {
            return jsonProvider.createObjectBuilder(variables).addAll(jsonProvider.createObjectBuilder(defaultValueObject)).build();
        }
        return variables;
    }

    private JsonValue valueToJsonValue(GraphqlParser.ValueContext valueContext) {
        if (valueContext.BooleanValue() != null) {
            return Boolean.parseBoolean(valueContext.BooleanValue().getText()) ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (valueContext.IntValue() != null) {
            return jsonProvider.createValue(Integer.parseInt(valueContext.IntValue().getText()));
        } else if (valueContext.FloatValue() != null) {
            return jsonProvider.createValue(Float.parseFloat(valueContext.FloatValue().getText()));
        } else if (valueContext.StringValue() != null) {
            return jsonProvider.createValue(DOCUMENT_UTIL.getStringValue(valueContext.StringValue()));
        } else if (valueContext.NullValue() != null) {
            return JsonValue.NULL;
        } else if (valueContext.enumValue() != null) {
            return jsonProvider.createValue(valueContext.enumValue().getText());
        } else if (valueContext.objectValue() != null) {
            return valueContext.objectValue().objectField().stream()
                    .map(objectFieldContext -> new AbstractMap.SimpleEntry<>(objectFieldContext.name().getText(), valueToJsonValue(objectFieldContext.value())))
                    .collect(JsonCollectors.toJsonObject());
        } else if (valueContext.arrayValue() != null) {
            return valueContext.arrayValue().value().stream().map(this::valueToJsonValue).collect(JsonCollectors.toJsonArray());
        }
        throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(valueContext.getText()));
    }

    private void processDirective(GraphqlParser.DirectiveContext directiveContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        if (directiveContext.arguments() != null) {
            directiveContext.arguments().argument()
                    .forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables));
        }
    }

    private void processFetchSelection(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.FieldContext fieldContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (fieldContext != null && fieldContext.selectionSet() != null) {
            fieldContext.selectionSet().selection().stream()
                    .map(selectionContext -> manager.getField(fieldTypeName, selectionContext.field().name().getText()))
                    .flatMap(Optional::stream)
                    .filter(manager::isFetchField)
                    .map(manager::getFetchFrom)
                    .map(fromFieldName -> manager.getField(fieldTypeName, fromFieldName))
                    .flatMap(Optional::stream)
                    .filter(fromFieldDefinitionContext -> fieldContext.selectionSet().selection().stream().noneMatch(selectionContext -> selectionContext.field().name().getText().equals(fromFieldDefinitionContext.name().getText())))
                    .findFirst()
                    .ifPresent(fromFieldDefinitionContext -> fieldContext.selectionSet().selection().add(DOCUMENT_UTIL.graphqlToSelection(fromFieldDefinitionContext.name().getText())));

            fieldContext.selectionSet().selection()
                    .forEach(subSelectionContext ->
                            processFetchSelection(
                                    manager.getField(manager.getFieldTypeName(fieldDefinitionContext.type()), subSelectionContext.field().name().getText())
                                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), subSelectionContext.field().name().getText()))),
                                    subSelectionContext.field()
                            )
                    );
        }
    }

    private void processFetchArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.FieldContext fieldContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (fieldContext != null && fieldContext.selectionSet() != null && fieldContext.arguments() != null) {
            fieldContext.arguments().argument().stream()
                    .map(argumentContext -> manager.getField(fieldTypeName, argumentContext.name().getText()))
                    .flatMap(Optional::stream)
                    .filter(manager::isFetchField)
                    .filter(fetchFieldDefinitionContext -> !manager.getFetchAnchor(fetchFieldDefinitionContext))
                    .map(manager::getFetchFrom)
                    .map(fromFieldName -> manager.getField(fieldTypeName, fromFieldName))
                    .flatMap(Optional::stream)
                    .filter(fromFieldDefinitionContext -> fieldContext.selectionSet().selection().stream().noneMatch(selectionContext -> selectionContext.field().name().getText().equals(fromFieldDefinitionContext.name().getText())))
                    .findFirst()
                    .ifPresent(fromFieldDefinitionContext -> fieldContext.selectionSet().selection().add(DOCUMENT_UTIL.graphqlToSelection(fromFieldDefinitionContext.name().getText())));

            fieldContext.selectionSet().selection()
                    .forEach(subSelectionContext ->
                            processFetchArgument(
                                    manager.getField(manager.getFieldTypeName(fieldDefinitionContext.type()), subSelectionContext.field().name().getText())
                                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), subSelectionContext.field().name().getText()))),
                                    subSelectionContext.field()
                            )
                    );
        }
    }

    private void processSelection(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        if (selectionContext.field() != null) {
            if (selectionContext.field().arguments() != null) {
                boolean skipNullArguments = skipNullArguments(selectionContext.field(), variables);
                selectionContext.field().arguments().argument()
                        .forEach(argumentContext -> replaceVariable(argumentContext.valueWithVariable(), operationDefinitionContext, variables, skipNullArguments));
                if (skipNullArguments) {
                    List<GraphqlParser.ArgumentContext> argumentContextList = selectionContext.field().arguments().argument().stream().filter(argumentContext -> argumentContext.getChildCount() > 0).collect(Collectors.toList());
                    ParseTree left = selectionContext.field().arguments().getChild(0);
                    ParseTree right = selectionContext.field().arguments().getChild(selectionContext.field().arguments().getChildCount() - 1);
                    IntStream.range(0, selectionContext.field().arguments().getChildCount()).forEach(index -> selectionContext.field().arguments().removeLastChild());
                    if (argumentContextList.size() > 0) {
                        selectionContext.field().arguments().addChild((TerminalNode) left);
                        for (GraphqlParser.ArgumentContext argumentContext : argumentContextList) {
                            selectionContext.field().arguments().addChild(argumentContext);
                        }
                        selectionContext.field().arguments().addChild((TerminalNode) right);
                    }
                }

                selectionContext.field().arguments().argument()
                        .forEach(argumentContext -> replaceEnumValue(getArgumentType(fieldDefinitionContext, argumentContext), argumentContext.valueWithVariable()));
            }
            if (selectionContext.field().selectionSet() != null) {
                selectionContext.field().selectionSet().selection()
                        .forEach(subSelectionContext ->
                                processSelection(
                                        manager.getObject(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_DEFINITION_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()))))
                                                .fieldsDefinition().fieldDefinition().stream()
                                                .filter(subFieldDefinitionContext -> subFieldDefinitionContext.name().getText().equals(subSelectionContext.field().name().getText()))
                                                .findFirst()
                                                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), subSelectionContext.field().name().getText()))),
                                        subSelectionContext,
                                        operationDefinitionContext,
                                        variables
                                )
                        );
            }
            if (selectionContext.field().directives() != null) {
                selectionContext.field().directives().directive().forEach(directiveContext -> processDirective(directiveContext, operationDefinitionContext, variables));
            }
        }
        if (selectionContext.fragmentSpread() != null) {
            if (selectionContext.fragmentSpread().directives() != null) {
                selectionContext.fragmentSpread().directives().directive().forEach(directiveContext -> processDirective(directiveContext, operationDefinitionContext, variables));
            }
        }
        if (selectionContext.inlineFragment() != null) {
            if (selectionContext.inlineFragment().directives() != null) {
                selectionContext.inlineFragment().directives().directive().forEach(directiveContext -> processDirective(directiveContext, operationDefinitionContext, variables));
            }
        }
    }

    private void replaceVariable(GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.OperationDefinitionContext operationDefinitionContext, Map<String, JsonValue> variables) {
        replaceVariable(valueWithVariableContext, operationDefinitionContext, variables, false);
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

    private void replaceEnumValue(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (manager.fieldTypeIsList(typeContext)) {
            if (valueWithVariableContext.arrayValueWithVariable() != null) {
                valueWithVariableContext.arrayValueWithVariable().valueWithVariable()
                        .forEach(subValueWithVariableContext -> replaceEnumValue(typeContext, subValueWithVariableContext));
            }
        } else if (manager.isInputObject(manager.getFieldTypeName(typeContext))) {
            if (valueWithVariableContext.objectValueWithVariable() != null) {
                valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable()
                        .forEach(objectFieldWithVariableContext -> replaceEnumValue(getObjectFieldWithVariableType(typeContext, objectFieldWithVariableContext), objectFieldWithVariableContext.valueWithVariable()));
            }
        } else {
            if (manager.isEnum(manager.getFieldTypeName(typeContext))) {
                if (valueWithVariableContext.StringValue() != null) {
                    GraphqlParser.EnumValueContext enumValueContext = DOCUMENT_UTIL.graphqlToEnumValue(DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue()));
                    valueWithVariableContext.removeLastChild();
                    valueWithVariableContext.addChild(enumValueContext);
                }
            }
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

    private GraphqlParser.TypeContext getArgumentType(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return Stream.ofNullable(fieldDefinitionContext.argumentsDefinition())
                .flatMap(argumentsDefinitionContext -> argumentsDefinitionContext.inputValueDefinition().stream())
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(argumentContext.name().getText()))
                .findFirst()
                .map(GraphqlParser.InputValueDefinitionContext::type)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind(argumentContext.name().getText())));
    }


    private GraphqlParser.TypeContext getObjectFieldWithVariableType(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return manager.getInputObject(manager.getFieldTypeName(typeContext))
                .orElseThrow(() -> new GraphQLErrors(INPUT_OBJECT_NOT_EXIST.bind(manager.getFieldTypeName(typeContext))))
                .inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(objectFieldWithVariableContext.name().getText()))
                .findFirst()
                .map(GraphqlParser.InputValueDefinitionContext::type)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind(objectFieldWithVariableContext.name().getText())));
    }
}
