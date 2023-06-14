package io.graphoenix.jsonpath.translator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.jsonpath.translator.expression.Expression;
import io.graphoenix.jsonpath.translator.expression.MultiAndExpression;
import io.graphoenix.jsonpath.translator.expression.MultiOrExpression;
import io.graphoenix.jsonpath.translator.expression.operators.*;
import io.graphoenix.jsonpath.translator.utils.JsonValueUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_VALUE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AFTER_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.BEFORE_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.GROUP_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.OFFSET_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.ORDER_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.SORT_INPUT_NAME;

@ApplicationScoped
public class GraphQLArgumentsToFilter {

    private final IGraphQLDocumentManager manager;
    private final JsonValueUtil jsonValueUtil;
    private final String[] EXCLUDE_INPUT = {DEPRECATED_INPUT_NAME, FIRST_INPUT_NAME, LAST_INPUT_NAME, OFFSET_INPUT_NAME, AFTER_INPUT_NAME, BEFORE_INPUT_NAME, GROUP_BY_INPUT_NAME, ORDER_BY_INPUT_NAME, SORT_INPUT_NAME, LIST_INPUT_NAME};

    @Inject
    public GraphQLArgumentsToFilter(IGraphQLDocumentManager manager, JsonValueUtil jsonValueUtil) {
        this.manager = manager;
        this.jsonValueUtil = jsonValueUtil;
    }

    public Optional<Expression> argumentsToMultipleExpression(GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                              GraphqlParser.ArgumentsContext argumentsContext) {
        if (argumentsContext == null) {
            return Optional.empty();
        }

        Stream<Expression> expressionStream = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(name -> name.equals(inputValueDefinitionContext.name().getText())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                .map(argumentContext ->
                                        objectValueWithVariableContextToExpression(inputValueDefinitionContext.name().getText(), argumentContext.valueWithVariable(), inputValueDefinitionContext)
                                )
                                .or(() ->
                                        Optional.ofNullable(inputValueDefinitionContext.defaultValue())
                                                .map(defaultValueContext -> objectValueContextToExpression(inputValueDefinitionContext.name().getText(), defaultValueContext.value(), inputValueDefinitionContext))
                                )
                                .orElse(Stream.empty())
                );

        if (hasOrConditional(argumentsContext, argumentsDefinitionContext)) {
            return Optional.of(new MultiOrExpression(expressionStream.collect(Collectors.toList())));
        } else {
            return Optional.of(new MultiAndExpression(expressionStream.collect(Collectors.toList())));
        }
    }

    protected Optional<Expression> objectValueWithVariableToMultipleExpression(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               String path) {
        if (objectValueWithVariableContext == null) {
            return Optional.empty();
        }

        Stream<Expression> expressionStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(name -> name.equals(inputValueDefinitionContext.name().getText())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                .map(objectFieldWithVariableContext ->
                                        objectValueWithVariableContextToExpression(path + "." + inputValueDefinitionContext.name().getText(), objectFieldWithVariableContext.valueWithVariable(), inputValueDefinitionContext)
                                )
                                .or(() ->
                                        Optional.ofNullable(inputValueDefinitionContext.defaultValue())
                                                .map(defaultValueContext -> objectValueContextToExpression(path + "." + inputValueDefinitionContext.name().getText(), defaultValueContext.value(), inputValueDefinitionContext))
                                )
                                .orElse(Stream.empty())
                );

        if (hasOrConditional(objectValueWithVariableContext, inputObjectTypeDefinitionContext)) {
            return Optional.of(new MultiOrExpression(expressionStream.collect(Collectors.toList())));
        } else {
            return Optional.of(new MultiAndExpression(expressionStream.collect(Collectors.toList())));
        }
    }

    protected Optional<Expression> objectValueToMultipleExpression(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext,
                                                                   String path) {
        if (objectValueContext == null) {
            return Optional.empty();
        }

        Stream<Expression> expressionStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(name -> name.equals(inputValueDefinitionContext.name().getText())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                .map(objectFieldContext ->
                                        objectValueContextToExpression(path + "." + inputValueDefinitionContext.name().getText(), objectFieldContext.value(), inputValueDefinitionContext)
                                )
                                .or(() ->
                                        Optional.ofNullable(inputValueDefinitionContext.defaultValue())
                                                .map(defaultValueContext -> objectValueContextToExpression(path + "." + inputValueDefinitionContext.name().getText(), defaultValueContext.value(), inputValueDefinitionContext))
                                )
                                .orElse(Stream.empty())
                );

        if (hasOrConditional(objectValueContext, inputObjectTypeDefinitionContext)) {
            return Optional.of(new MultiOrExpression(expressionStream.collect(Collectors.toList())));
        } else {
            return Optional.of(new MultiAndExpression(expressionStream.collect(Collectors.toList())));
        }
    }

    protected Stream<Expression> objectValueWithVariableContextToExpression(String element, GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
        if (manager.isInputObject(fieldTypeName)) {
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                return Stream.ofNullable(valueWithVariableContext.arrayValueWithVariable())
                        .flatMap(arrayValueWithVariableContext -> arrayValueWithVariableContext.valueWithVariable().stream())
                        .flatMap(itemValueWithVariableContext -> objectValueWithVariableContextToExpression(element, itemValueWithVariableContext, inputValueDefinitionContext));
            } else {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return getOperatorExpression(element, valueWithVariableContext.objectValueWithVariable(), inputValueDefinitionContext).stream();
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                            .flatMap(inputObjectTypeDefinitionContext -> objectValueWithVariableToMultipleExpression(inputObjectTypeDefinitionContext, valueWithVariableContext.objectValueWithVariable(), element))
                            .stream();
                }
            }
        }
        return Stream.empty();
    }

    protected Stream<Expression> objectValueContextToExpression(String element, GraphqlParser.ValueContext valueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
        if (manager.isInputObject(fieldTypeName)) {
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                return Stream.ofNullable(valueContext.arrayValue())
                        .flatMap(arrayValueContext -> arrayValueContext.value().stream())
                        .flatMap(itemValueContext -> objectValueContextToExpression(element, itemValueContext, inputValueDefinitionContext));
            } else {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return getOperatorExpression(element, valueContext.objectValue(), inputValueDefinitionContext).stream();
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                            .flatMap(inputObjectTypeDefinitionContext -> objectValueToMultipleExpression(inputObjectTypeDefinitionContext, valueContext.objectValue(), element))
                            .stream();
                }
            }
        }
        return Stream.empty();
    }

    private Optional<Expression> getOperatorExpression(String element, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                .flatMap(inputObjectTypeDefinition ->
                        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && !manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                                .findFirst()
                                .map(fieldInputValueDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, fieldInputValueDefinitionContext)
                                                .map(objectFieldContext ->
                                                        operatorToExpression(
                                                                element,
                                                                getOperator(objectValueWithVariableContext, inputValueDefinitionContext),
                                                                objectFieldContext.valueWithVariable()
                                                        )
                                                )
                                                .orElse(
                                                        Optional.ofNullable(fieldInputValueDefinitionContext.defaultValue())
                                                                .flatMap(defaultValueContext ->
                                                                        operatorToExpression(
                                                                                element,
                                                                                getOperator(objectValueWithVariableContext, inputValueDefinitionContext),
                                                                                defaultValueContext.value()
                                                                        )
                                                                )
                                                )
                                )
                )
                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))));
    }

    private Optional<Expression> getOperatorExpression(String element, GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                .flatMap(inputObjectTypeDefinition ->
                        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && !manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                                .findFirst()
                                .map(fieldInputValueDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, fieldInputValueDefinitionContext)
                                                .map(objectFieldContext ->
                                                        operatorToExpression(
                                                                element,
                                                                getOperator(objectValueContext, inputValueDefinitionContext),
                                                                objectFieldContext.value()
                                                        )
                                                )
                                                .orElse(
                                                        Optional.ofNullable(fieldInputValueDefinitionContext.defaultValue())
                                                                .flatMap(defaultValueContext ->
                                                                        operatorToExpression(
                                                                                element,
                                                                                getOperator(objectValueContext, inputValueDefinitionContext),
                                                                                defaultValueContext.value()
                                                                        )
                                                                )
                                                )
                                )
                )
                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))));
    }

    private GraphqlParser.EnumValueContext getOperator(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                .flatMap(inputObjectTypeDefinition ->
                        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                                .findFirst()
                                .map(fieldInputValueDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, fieldInputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue())
                                                .orElse(fieldInputValueDefinitionContext.defaultValue().value().enumValue())
                                )
                )
                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))));
    }

    private GraphqlParser.EnumValueContext getOperator(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                .flatMap(inputObjectTypeDefinition ->
                        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                                .findFirst()
                                .map(fieldInputValueDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, fieldInputValueDefinitionContext)
                                                .map(objectFieldContext -> objectFieldContext.value().enumValue())
                                                .orElse(fieldInputValueDefinitionContext.defaultValue().value().enumValue())
                                )
                )
                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))));
    }

    private Optional<Expression> operatorToExpression(String element,
                                                      GraphqlParser.EnumValueContext operator,
                                                      GraphqlParser.ValueContext valueContext) {
        Expression expression;
        switch (operator.enumValueName().getText()) {
            case "EQ":
                expression = new EqualsTo(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "NEQ":
                expression = new NotEqualsTo(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "LK":
                expression = new Like(element, DOCUMENT_UTIL.getStringValue(valueContext.StringValue()));
                break;
            case "NLK":
                expression = new NotLike(element, DOCUMENT_UTIL.getStringValue(valueContext.StringValue()));
                break;
            case "GT":
            case "NLTE":
                expression = new GreaterThan(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "GTE":
            case "NLT":
                expression = new GreaterThanEquals(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "LT":
            case "NGTE":
                expression = new MinorThan(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "LTE":
            case "NGT":
                expression = new MinorThanEquals(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "NIL":
                expression = new IsNullExpression(element);
                break;
            case "NNIL":
                expression = new NotNullExpression(element);
                break;
            case "IN":
                expression = new InExpression(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            case "NIN":
                expression = new NotInExpression(element, jsonValueUtil.valueToJsonValue(valueContext));
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
        return Optional.of(expression);
    }

    private Optional<Expression> operatorToExpression(String element,
                                                      GraphqlParser.EnumValueContext operator,
                                                      GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Expression expression;
        switch (operator.enumValueName().getText()) {
            case "EQ":
                expression = new EqualsTo(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "NEQ":
                expression = new NotEqualsTo(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "LK":
                expression = new Like(element, DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue()));
                break;
            case "NLK":
                expression = new NotLike(element, DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue()));
                break;
            case "GT":
            case "NLTE":
                expression = new GreaterThan(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "GTE":
            case "NLT":
                expression = new GreaterThanEquals(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "LT":
            case "NGTE":
                expression = new MinorThan(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "LTE":
            case "NGT":
                expression = new MinorThanEquals(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "NIL":
                expression = new IsNullExpression(element);
                break;
            case "NNIL":
                expression = new NotNullExpression(element);
                break;
            case "IN":
                expression = new InExpression(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            case "NIN":
                expression = new NotInExpression(element, jsonValueUtil.valueWithVariableToJsonValue(valueWithVariableContext));
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
        return Optional.of(expression);
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream()
                .anyMatch(argumentContext ->
                        manager.getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                                .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                                .orElse(false)
                );
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .anyMatch(objectFieldWithVariableContext ->
                        manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                                .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                                .orElse(false)
                );
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
                        .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, objectFieldContext.value()))
                        .orElse(false));
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueWithVariableContext.enumValue());
        }
        return false;
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueContext.enumValue());
        }
        return false;
    }

    private boolean conditionalIsOr(GraphqlParser.EnumValueContext enumValueContext) {
        return enumValueContext != null && enumValueContext.enumValueName().getText().equals("OR");
    }

    private boolean isConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return inputValueDefinitionContext.type().typeName() != null && isConditional(inputValueDefinitionContext.type().typeName().name().getText());
    }

    private boolean isConditional(String typeName) {
        return typeName != null && manager.isEnum(typeName) && typeName.equals("Conditional");
    }

    private boolean isOperatorObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Operator");
    }

    private boolean isConditionalObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Conditional");
    }

    private boolean containsEnum(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, String enumName) {
        String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> objectTypeDefinition = manager.getInputObject(fieldTypeName);
        return objectTypeDefinition
                .map(inputObjectTypeDefinitionContext ->
                        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .anyMatch(fieldInputValueDefinitionContext ->
                                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals(enumName))
                )
                .orElse(false);
    }
}

