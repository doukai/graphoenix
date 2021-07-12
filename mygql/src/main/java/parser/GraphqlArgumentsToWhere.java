package parser;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlArgumentsToWhere {

    private final GraphqlAntlrRegister register;

    public GraphqlArgumentsToWhere(GraphqlAntlrRegister register) {
        this.register = register;
    }

    protected Expression argumentsToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Stream<Expression> expressionStream = argumentsToExpressionList(fieldTypeContext, argumentsDefinitionContext, argumentsContext);
        return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(argumentsContext, argumentsDefinitionContext));
    }

    protected Expression objectValueWithVariableToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext.type());
        Stream<Expression> expressionStream = objectValueWithVariableToExpressionList(fieldTypeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueWithVariableContext);
        return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueWithVariableContext, typeDefinitionContext.inputObjectTypeDefinition()));
    }

    protected Expression objectValueToMultipleExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext.type());
        Stream<Expression> expressionStream = objectValueToExpressionList(typeContext, typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions(), objectValueContext);
        return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueContext, typeDefinitionContext.inputObjectTypeDefinition()));
    }

    protected Expression expressionStreamToMultipleExpression(Stream<Expression> expressionStream, boolean hasOrConditional) {
        List<Expression> expressionList = expressionStream.collect(Collectors.toList());
        if (expressionList.size() == 1) {
            return expressionList.get(0);
        }
        if (hasOrConditional) {
            return new MultiOrExpression(expressionList);
        }
        return new MultiAndExpression(expressionList);
    }

    protected Stream<Expression> argumentsToExpressionList(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Stream<Expression> expressionStream = argumentsDefinitionContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> argumentsToExpression(typeContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfArgumentsToExpressionList(typeContext, argumentsDefinitionContext, argumentsContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> objectValueWithVariableToExpression(typeContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(typeContext, inputObjectValueDefinitionsContext, objectValueWithVariableContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueToExpressionList(GraphqlParser.TypeContext typeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(this::isNotConditional).map(inputValueDefinitionContext -> objectValueToExpression(typeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueToExpression(typeContext, inputObjectValueDefinitionsContext, objectValueContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Optional<Expression> argumentsToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return argumentToExpression(typeContext, inputValueDefinitionContext, argumentContext.get());
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueWithVariableToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return objectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.get());
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return objectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext.get());
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> defaultValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueToExpression(typeContext, inputValueDefinitionContext);
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> argumentToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return argumentContext == null ? listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext);
        } else {
            return argumentContext == null ? singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> inputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldWithVariableContext == null ? listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        } else {
            return objectFieldWithVariableContext == null ? singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> objectFieldToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (register.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldContext == null ? listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        } else {
            return objectFieldContext == null ? singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Optional<Expression> singleTypeArgumentToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {

        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getFieldTypeDefinition(typeContext).objectTypeDefinition();
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorArgumentToExpression(argumentToColumn(typeContext, argumentContext), inputValueDefinitionContext, argumentContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext), argumentContext.valueWithVariable()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getFieldTypeDefinition(typeContext).objectTypeDefinition();
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorObjectFieldWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext), inputValueDefinitionContext, objectFieldWithVariableContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext), objectFieldWithVariableContext.valueWithVariable()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getFieldTypeDefinition(typeContext).objectTypeDefinition();
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext,inputValueDefinitionContext);

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldContext.value().objectValue()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorObjectFieldToExpression(objectFieldToColumn(typeContext, objectFieldContext), inputValueDefinitionContext, objectFieldContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueToExpression(objectFieldToColumn(typeContext, objectFieldContext), objectFieldContext.value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeInputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getFieldTypeDefinition(typeContext).objectTypeDefinition();
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext,inputValueDefinitionContext);

        if (fieldDefinitionContext.isPresent()) {
            String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.get().type());
            if (register.isObject(fieldTypeName)) {
                if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToExpression(objectTypeDefinition, fieldDefinitionContext.get(), inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue()));
                } else {
                    //todo
                }
            } else if (register.isScaLar(fieldTypeName) || register.isInnerScalar(fieldTypeName)) {
                if (isOperatorObject(inputValueDefinitionContext)) {
                    return operatorInputValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext), inputValueDefinitionContext);
                } else if (isConditionalObject(inputValueDefinitionContext)) {
                    return Optional.of(objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue()));
                } else if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return Optional.of(scalarValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext), inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> listTypeArgumentToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeArgumentToInExpression(typeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> listTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (objectFieldWithVariableContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeObjectFieldWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> listTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (objectFieldContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeObjectFieldToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Stream<Expression> listTypeConditionalFieldOfArgumentsToExpressionList(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = argumentsDefinitionContext.inputValueDefinition().stream().filter(inputValueDefinitionContext -> register.fieldTypeIsList(inputValueDefinitionContext.type()) && isConditionalObject(inputValueDefinitionContext)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, conditionalInputValueDefinition.get());
            return argumentContext.flatMap(context -> Optional.of(context.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream().map(valueWithVariableContext ->
                    objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinition.get(), valueWithVariableContext.objectValueWithVariable())))).orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinition.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, conditionalInputValueDefinition.get());
            return objectFieldWithVariableContext.flatMap(fieldWithVariableContext -> Optional.of(fieldWithVariableContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream().map(valueWithVariableContext ->
                    objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinition.get(), valueWithVariableContext.objectValueWithVariable())))).orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinition.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinition = inputObjectValueDefinitionsContext.inputValueDefinition().stream().filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext)).findFirst();
        if (conditionalInputValueDefinition.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, conditionalInputValueDefinition.get());
            return objectFieldContext.flatMap(fieldContext -> Optional.of(fieldContext.value().arrayValue().value().stream().map(valueContext ->
                    objectValueToMultipleExpression(typeContext, conditionalInputValueDefinition.get(), valueContext.objectValue())))).orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinition.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfInputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream().map(valueContext ->
                        objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, valueContext.objectValue()));
            } else {
                //todo
            }
        }
        return Stream.empty();
    }

    private Optional<Expression> listTypeArgumentToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> listTypeInputValueToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> listTypeObjectFieldWithVariableToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> listTypeObjectFieldToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value());
    }

    protected Optional<Expression> valueWithVariableToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueWithVariableContext.arrayValueWithVariable() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream().map(register::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> valueToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueContext.arrayValue() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(register::scalarValueToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream().anyMatch(argumentContext ->
                register.getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream().anyMatch(objectFieldWithVariableContext ->
                register.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                register.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldContext.value()))
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

    private boolean isNotConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return !isConditional(inputValueDefinitionContext);
    }

    private boolean isConditional(String typeName) {
        return typeName != null && register.isEnum(typeName) && typeName.equals("Conditional");
    }

    private boolean isOperatorObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Operator");
    }

    private boolean isConditionalObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Conditional");
    }

    private boolean containsEnum(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, String enumName) {
        String fieldTypeName = register.getFieldTypeName(inputValueDefinitionContext.type());
        if (register.isInputObject(fieldTypeName)) {
            GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(fieldTypeName);
            return typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .anyMatch(fieldInputValueDefinitionContext ->
                            register.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                    fieldInputValueDefinitionContext.type().typeName().name().getText().equals(enumName));
        }
        return false;
    }

    private Optional<Expression> operatorArgumentToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> operatorInputValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> operatorObjectFieldWithVariableToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> operatorObjectFieldToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, objectFieldContext.value());
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext.type());
        Optional<GraphqlParser.ObjectFieldWithVariableContext> enumField = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        register.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator")).findFirst().flatMap(fieldInputValueDefinitionContext -> register.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), fieldInputValueDefinitionContext));

        Optional<GraphqlParser.InputValueDefinitionContext> valueFieldType = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        !register.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator")).findFirst();

        if (enumField.isPresent() && valueFieldType.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> valueField = register.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), valueFieldType.get());
            if (valueField.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, enumField.get().valueWithVariable().enumValue(), valueField.get().valueWithVariable());
            } else if (valueFieldType.get().type().nonNullType() != null) {
                if (valueFieldType.get().defaultValue() != null) {
                    return operatorValueToExpression(leftExpression, enumField.get().valueWithVariable().enumValue(), valueFieldType.get().defaultValue().value());
                } else {
                    //todo
                }
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext.type());
        Optional<GraphqlParser.ObjectFieldContext> enumField = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        register.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator")).findFirst().flatMap(fieldInputValueDefinitionContext -> register.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), fieldInputValueDefinitionContext));

        Optional<GraphqlParser.InputValueDefinitionContext> valueFieldType = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        !register.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator")).findFirst();

        if (enumField.isPresent() && valueFieldType.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> valueField = register.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), valueFieldType.get());
            if (valueField.isPresent()) {
                return operatorValueToExpression(leftExpression, enumField.get().value().enumValue(), valueField.get().value());
            } else if (valueFieldType.get().type().nonNullType() != null) {
                if (valueFieldType.get().defaultValue() != null) {
                    return operatorValueToExpression(leftExpression, enumField.get().value().enumValue(), valueFieldType.get().defaultValue().value());
                } else {
                    //todo
                }
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return Optional.ofNullable(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, valueWithVariableContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(leftExpression, enumValueContext, valueWithVariableContext.StringValue(), valueWithVariableContext.IntValue(), valueWithVariableContext.FloatValue(), valueWithVariableContext.BooleanValue(), valueWithVariableContext.NullValue()));
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueContext valueContext) {
        if (valueContext.arrayValue() != null) {
            return Optional.ofNullable(operatorValueToInExpression(leftExpression, enumValueContext, valueContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(leftExpression, enumValueContext, valueContext.StringValue(), valueContext.IntValue(), valueContext.FloatValue(), valueContext.BooleanValue(), valueContext.NullValue()));
    }

    private Expression operatorValueToInExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueContext valueContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(register::scalarValueToDBValue).collect(Collectors.toList())));
        if ("IN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(false);
        } else if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        } else {
            //todo
            return null;
        }
        return inExpression;
    }

    private Expression operatorValueWithVariableToInExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream().map(register::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
        if ("IN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(false);
        } else if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        } else {
            //todo
            return null;
        }
        return inExpression;
    }

    private Expression operatorScalarValueToExpression(Expression leftExpression, GraphqlParser.EnumValueContext enumValueContext, TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        switch (enumValueContext.enumValueName().getText()) {
            case "EQ":
                return scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue);
            case "NEQ":
                return new NotExpression(scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(register.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThanEquals;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                return isNullExpression;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                return isNotNullExpression;
            default:
                return null;
        }
    }

    protected Expression objectValueWithVariableToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.type());
        ExistsExpression expression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));
        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
        Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(fieldTypeName));
        body.setFromItem(subTable);
        EqualsTo idEqualsTo = new EqualsTo();
        if (register.fieldTypeIsList(fieldDefinitionContext.type())) {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(fieldTypeName, objectTypeDefinitionContext.name().getText()))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(objectTypeDefinitionContext.name().getText()))));
        } else {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(fieldTypeName))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
        }
        Expression subWhereExpression = objectValueWithVariableToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueWithVariableContext);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(idEqualsTo, subWhereExpression));
        body.setWhere(multiAndExpression);
        subSelect.setSelectBody(body);
        expression.setRightExpression(subSelect);
        return expression;
    }

    protected Expression objectValueToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        String fieldTypeName = register.getFieldTypeName(fieldDefinitionContext.type());
        ExistsExpression expression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));
        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(objectTypeDefinitionContext.name().getText()));
        Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(fieldTypeName));
        body.setFromItem(subTable);
        EqualsTo idEqualsTo = new EqualsTo();
        if (register.fieldTypeIsList(fieldDefinitionContext.type())) {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(fieldTypeName, objectTypeDefinitionContext.name().getText()))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(objectTypeDefinitionContext.name().getText()))));
        } else {
            idEqualsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(fieldTypeName))));
            idEqualsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
        }
        Expression subWhereExpression = objectValueToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueContext);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(idEqualsTo, subWhereExpression));
        body.setWhere(multiAndExpression);
        subSelect.setSelectBody(body);
        expression.setRightExpression(subSelect);
        return expression;
    }

    protected Column argumentToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext argumentContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    protected Column inputValueToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    protected Column objectFieldWithVariableToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    protected Column objectFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    protected Expression scalarValueToExpression(Expression leftExpression, GraphqlParser.ValueContext valueContext) {
        return scalarValueToExpression(leftExpression,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    protected Expression scalarValueWithVariableToExpression(Expression leftExpression, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToExpression(leftExpression,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    protected Expression scalarValueToExpression(Expression leftExpression, TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText())));
            return equalsTo;
        } else if (intValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new LongValue(intValue.getText()));
            return equalsTo;
        } else if (floatValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new DoubleValue(floatValue.getText()));
            return equalsTo;
        } else if (booleanValue != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(booleanValue.getText()));
            return isBooleanExpression;
        } else if (nullValue != null) {
            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.setLeftExpression(leftExpression);
            return isNullExpression;
        }
        return null;
    }
}
