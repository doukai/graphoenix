package io.graphoenix.mysql.translator;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.meta.antlr.IGraphqlDocumentManager;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Join;
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

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlArgumentsToWhere {

    private final IGraphqlDocumentManager manager;

    public GraphqlArgumentsToWhere(IGraphqlDocumentManager manager) {
        this.manager = manager;
    }

    protected Optional<Expression> argumentsToMultipleExpression(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext) {
        Stream<Expression> expressionStream = argumentsToExpressionList(fieldDefinitionContext.type(), fieldDefinitionContext.argumentsDefinition(), argumentsContext);
        return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(argumentsContext, fieldDefinitionContext.argumentsDefinition()));
    }

    protected Optional<Expression> objectValueWithVariableToMultipleExpression(GraphqlParser.TypeContext fieldTypeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        if (inputObjectTypeDefinitionContext.isPresent()) {
            Stream<Expression> expressionStream = objectValueWithVariableToExpressionList(fieldTypeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions(), objectValueWithVariableContext);
            return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueWithVariableContext, inputObjectTypeDefinitionContext.get()));
        }
        return Optional.empty();
    }

    protected Optional<Expression> objectValueToMultipleExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        if (inputObjectTypeDefinitionContext.isPresent()) {
            Stream<Expression> expressionStream = objectValueToExpressionList(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions(), objectValueContext);
            return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueContext, inputObjectTypeDefinitionContext.get()));
        }
        return Optional.empty();
    }

    protected Optional<Expression> expressionStreamToMultipleExpression(Stream<Expression> expressionStream, boolean hasOrConditional) {
        List<Expression> expressionList = expressionStream.collect(Collectors.toList());
        if (expressionList.size() == 0) {
            return Optional.empty();
        } else if (expressionList.size() == 1) {
            return Optional.of(expressionList.get(0));
        } else {
            if (hasOrConditional) {
                return Optional.of(new MultiOrExpression(expressionList));
            } else {
                return Optional.of(new MultiAndExpression(expressionList));
            }
        }
    }

    protected Stream<Expression> argumentsToExpressionList(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext) {
        Stream<Expression> expressionStream = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .map(inputValueDefinitionContext -> argumentsToExpression(typeContext, inputValueDefinitionContext, argumentsContext))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfArgumentsToExpressionList(typeContext, argumentsDefinitionContext, argumentsContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                         GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .map(inputValueDefinitionContext -> objectValueWithVariableToExpression(typeContext, inputValueDefinitionContext, objectValueWithVariableContext))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(typeContext, inputObjectValueDefinitionsContext, objectValueWithVariableContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueToExpressionList(GraphqlParser.TypeContext typeContext,
                                                             GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                             GraphqlParser.ObjectValueContext objectValueContext) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .map(inputValueDefinitionContext -> objectValueToExpression(typeContext, inputValueDefinitionContext, objectValueContext))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueToExpression(typeContext, inputObjectValueDefinitionsContext, objectValueContext);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Optional<Expression> argumentsToExpression(GraphqlParser.TypeContext typeContext,
                                                         GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                         GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return argumentToExpression(typeContext, inputValueDefinitionContext, argumentContext.get());
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return objectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.get());
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectValueToExpression(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
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

    protected Optional<Expression> argumentToExpression(GraphqlParser.TypeContext typeContext,
                                                        GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                        GraphqlParser.ArgumentContext argumentContext) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return argumentContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext);
        } else {
            return argumentContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> inputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> objectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldWithVariableContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        } else {
            return objectFieldWithVariableContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> objectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext) : listTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        } else {
            return objectFieldContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext) : singleTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Optional<Expression> singleTypeArgumentToExpression(GraphqlParser.TypeContext typeContext,
                                                                  GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                  GraphqlParser.ArgumentContext argumentContext) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueWithVariableToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                argumentContext.valueWithVariable().objectValueWithVariable())
                        );
                    } else {
                        //todo
                    }
                } else if (manager.isScaLar(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorArgumentToExpression(argumentToColumn(typeContext, argumentContext), inputValueDefinitionContext, argumentContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(argumentToColumn(typeContext, argumentContext), argumentContext.valueWithVariable(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable());
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        return Optional.of(scalarValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext), argumentContext.valueWithVariable()));
                    } else {
                        //todo
                    }
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                                 GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {

            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueWithVariableToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable())
                        );
                    } else {
                        //todo
                    }
                } else if (manager.isScaLar(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorObjectFieldWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext), inputValueDefinitionContext, objectFieldWithVariableContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext), objectFieldWithVariableContext.valueWithVariable(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable());
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        return Optional.of(scalarValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext), objectFieldWithVariableContext.valueWithVariable()));
                    } else {
                        //todo
                    }
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                                     GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                     GraphqlParser.ObjectFieldContext objectFieldContext) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                objectFieldContext.value().objectValue())
                        );
                    } else {
                        //todo
                    }
                } else if (manager.isScaLar(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorObjectFieldToExpression(objectFieldToColumn(typeContext, objectFieldContext), inputValueDefinitionContext, objectFieldContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(objectFieldToColumn(typeContext, objectFieldContext), objectFieldContext.value(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue());
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        return Optional.of(scalarValueToExpression(objectFieldToColumn(typeContext, objectFieldContext), objectFieldContext.value()));
                    } else {
                        //todo
                    }
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeInputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinition = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinition.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueToExpression(
                                objectTypeDefinition.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                inputValueDefinitionContext.defaultValue().value().objectValue())
                        );
                    } else {
                        //todo
                    }
                } else if (manager.isScaLar(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorInputValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext), inputValueDefinitionContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(inputValueToColumn(typeContext, inputValueDefinitionContext), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue());
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        return Optional.of(scalarValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext), inputValueDefinitionContext.defaultValue().value()));
                    } else {
                        //todo
                    }
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> listTypeArgumentToExpression(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeArgumentToInExpression(typeContext, inputValueDefinitionContext, argumentContext);
        }
    }

    protected Optional<Expression> listTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (objectFieldWithVariableContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeObjectFieldWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext);
        }
    }

    protected Optional<Expression> listTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (objectFieldContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext);
        } else {
            return listTypeObjectFieldToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext);
        }
    }

    protected Stream<Expression> listTypeConditionalFieldOfArgumentsToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                                     GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                                                     GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()) && isConditionalObject(inputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, conditionalInputValueDefinitionContext.get());
            return argumentContext
                    .flatMap(context -> Optional.of(context.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)))
                    .orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                                                   GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, conditionalInputValueDefinitionContext.get());
            return objectFieldWithVariableContext
                    .flatMap(fieldWithVariableContext -> Optional.of(fieldWithVariableContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)))
                    .orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueToExpression(GraphqlParser.TypeContext typeContext,
                                                                                   GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                                   GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, conditionalInputValueDefinitionContext.get());
            return objectFieldContext.flatMap(fieldContext -> Optional.of(fieldContext.value().arrayValue().value().stream()
                    .map(valueContext -> objectValueToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueContext.objectValue()))
                    .filter(Optional::isPresent)
                    .map(Optional::get))
            ).orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get()));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfInputValueToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
                        .map(valueContext -> objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, valueContext.objectValue()))
                        .filter(Optional::isPresent)
                        .map(Optional::get);
            } else {
                //todo
            }
        }
        return Stream.empty();
    }

    private Optional<Expression> listTypeArgumentToInExpression(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                GraphqlParser.ArgumentContext argumentContext) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> listTypeInputValueToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> listTypeObjectFieldWithVariableToInExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> listTypeObjectFieldToInExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectFieldContext objectFieldContext) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value());
    }

    protected Optional<Expression> valueWithVariableToInExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueWithVariableContext.arrayValueWithVariable() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> valueToInExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueContext.arrayValue() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext));
                inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(DB_VALUE_UTIL::scalarValueToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream().anyMatch(argumentContext ->
                manager.getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream().anyMatch(objectFieldWithVariableContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
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
        return objectTypeDefinition.map(inputObjectTypeDefinitionContext -> inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .anyMatch(fieldInputValueDefinitionContext ->
                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals(enumName))).orElse(false);
    }

    private Optional<Expression> operatorArgumentToExpression(Expression leftExpression,
                                                              GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                              GraphqlParser.ArgumentContext argumentContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> operatorInputValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> operatorObjectFieldWithVariableToExpression(Expression leftExpression,
                                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                             GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> operatorObjectFieldToExpression(Expression leftExpression,
                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                 GraphqlParser.ObjectFieldContext objectFieldContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, objectFieldContext.value());
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (inputObjectTypeDefinition.isPresent()) {
            Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), fieldInputValueDefinitionContext))
                    .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue());

            Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition)
                    .map(GraphqlParser.ValueContext::enumValue);

            Optional<GraphqlParser.ValueWithVariableContext> subValueWithVariableContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                    !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(subInputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), subInputValueDefinitionContext))
                    .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable);

            Optional<GraphqlParser.ValueContext> subDefaultValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                    !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition);

            if (operatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, operatorEnumValueContext.get(), subValueWithVariableContext.get());
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subDefaultValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subValueWithVariableContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subDefaultValueContext.get());
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ValueContext valueContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (inputObjectTypeDefinition.isPresent()) {
            Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), fieldInputValueDefinitionContext))
                    .map(objectFieldContext -> objectFieldContext.value().enumValue());

            Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition)
                    .map(GraphqlParser.ValueContext::enumValue);

            Optional<GraphqlParser.ValueContext> subValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                    !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(subInputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), subInputValueDefinitionContext))
                    .map(GraphqlParser.ObjectFieldContext::value);

            Optional<GraphqlParser.ValueContext> subDefaultValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !manager.isEnum(fieldInputValueDefinitionContext.type().getText()) ||
                                    !fieldInputValueDefinitionContext.type().typeName().name().getText().equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition);

            if (operatorEnumValueContext.isPresent() && subValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subValueContext.get());
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subDefaultValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subDefaultValueContext.get());
            }
        }
        //todo
        return Optional.empty();
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.EnumValueContext enumValueContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return Optional.ofNullable(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, valueWithVariableContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue())
        );
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.EnumValueContext enumValueContext,
                                                           GraphqlParser.ValueContext valueContext) {
        if (valueContext.arrayValue() != null) {
            return Optional.ofNullable(operatorValueToInExpression(leftExpression, enumValueContext, valueContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue())
        );
    }

    private Expression operatorValueToInExpression(Expression leftExpression,
                                                   GraphqlParser.EnumValueContext enumValueContext,
                                                   GraphqlParser.ValueContext valueContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream()
                .map(DB_VALUE_UTIL::scalarValueToDBValue).collect(Collectors.toList())));
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

    private Expression operatorValueWithVariableToInExpression(Expression leftExpression,
                                                               GraphqlParser.EnumValueContext enumValueContext,
                                                               GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
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

    private Expression operatorScalarValueToExpression(Expression leftExpression,
                                                       GraphqlParser.EnumValueContext enumValueContext,
                                                       TerminalNode stringValue,
                                                       TerminalNode intValue,
                                                       TerminalNode floatValue,
                                                       TerminalNode booleanValue,
                                                       TerminalNode nullValue) {
        switch (enumValueContext.enumValueName().getText()) {
            case "EQ":
                return scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue);
            case "NEQ":
                return new NotExpression(scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
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

    protected Expression objectValueWithVariableToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                             GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext);
        Optional<Expression> subWhereExpression = objectValueWithVariableToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueWithVariableContext);
        subWhereExpression.ifPresent(expression -> {
            if (body.getWhere() != null) {
                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
            } else {
                body.setWhere(expression);
            }
        });
        return existsExpression(body);
    }

    protected Expression objectValueToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                 GraphqlParser.ObjectValueContext objectValueContext) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext);
        Optional<Expression> subWhereExpression = objectValueToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueContext);
        subWhereExpression.ifPresent(expression -> {
            if (body.getWhere() != null) {
                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
            } else {
                body.setWhere(expression);
            }
        });
        return existsExpression(body);
    }


    protected PlainSelect mapFieldPlainSelect(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = DB_NAME_UTIL.typeToTable(objectTypeDefinitionContext);
        Table subTable = DB_NAME_UTIL.typeToTable(fieldTypeName);
        body.setFromItem(subTable);

        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext);
        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);

        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTypeTable = DB_NAME_UTIL.typeToTable(mapWithTypeName.get());

                    Join joinWithTable = new Join();
                    EqualsTo joinWithTableEqualsColumn = new EqualsTo();
                    joinWithTableEqualsColumn.setLeftExpression(DB_NAME_UTIL.fieldToColumn(withTypeTable, mapWithToFieldName.get()));
                    joinWithTableEqualsColumn.setLeftExpression(DB_NAME_UTIL.fieldToColumn(subTable, toFieldDefinition.get().name().getText()));
                    joinWithTable.setLeft(true);
                    joinWithTable.setRightItem(withTypeTable);
                    joinWithTable.setOnExpression(joinWithTableEqualsColumn);

                    Join joinParentTable = new Join();
                    EqualsTo joinTableEqualsParentColumn = new EqualsTo();
                    joinWithTableEqualsColumn.setLeftExpression(DB_NAME_UTIL.fieldToColumn(withTypeTable, mapWithFromFieldName.get()));
                    joinWithTableEqualsColumn.setLeftExpression(DB_NAME_UTIL.fieldToColumn(table, fromFieldDefinition.get().name().getText()));
                    joinParentTable.setLeft(true);
                    joinParentTable.setRightItem(table);
                    joinParentTable.setOnExpression(joinTableEqualsParentColumn);

                    body.setJoins(Arrays.asList(joinWithTable, joinParentTable));
                }
            } else {
                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(DB_NAME_UTIL.fieldToColumn(subTable, toFieldDefinition.get()));
                idEqualsTo.setRightExpression(DB_NAME_UTIL.fieldToColumn(table, fromFieldDefinition.get()));

                body.setWhere(idEqualsTo);
            }
        }
        return body;
    }

    protected Column argumentToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext argumentContext) {
        return DB_NAME_UTIL.fieldToColumn(manager.getFieldTypeName(typeContext), argumentContext);
    }

    protected Column inputValueToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return DB_NAME_UTIL.fieldToColumn(manager.getFieldTypeName(typeContext), inputValueDefinitionContext);
    }

    protected Column objectFieldWithVariableToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return DB_NAME_UTIL.fieldToColumn(manager.getFieldTypeName(typeContext), objectFieldWithVariableContext);
    }

    protected Column objectFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return DB_NAME_UTIL.fieldToColumn(manager.getFieldTypeName(typeContext), objectFieldContext);
    }

    protected Expression scalarValueToExpression(Expression leftExpression, GraphqlParser.ValueContext valueContext) {
        return scalarValueToExpression(leftExpression,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue());
    }

    protected Expression scalarValueWithVariableToExpression(Expression
                                                                     leftExpression, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToExpression(leftExpression,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue());
    }

    protected Expression scalarValueToExpression(Expression leftExpression,
                                                 TerminalNode stringValue,
                                                 TerminalNode intValue,
                                                 TerminalNode floatValue,
                                                 TerminalNode booleanValue,
                                                 TerminalNode nullValue) {
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

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (valueWithVariableContext.BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(valueWithVariableContext.BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else {
            return isBooleanExpression(leftExpression, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.ValueContext valueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (valueContext.BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(valueContext.BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else {
            return isBooleanExpression(leftExpression, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
                isBooleanExpression.setLeftExpression(leftExpression);
                isBooleanExpression.setIsTrue(Boolean.parseBoolean(inputValueDefinitionContext.defaultValue().value().BooleanValue().getText()));
                return Optional.of(isBooleanExpression);
            } else {
                //TODO
            }
        }
        return Optional.empty();
    }

    protected ExistsExpression existsExpression(PlainSelect body) {
        ExistsExpression existsExpression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(body);
        existsExpression.setRightExpression(subSelect);
        return existsExpression;
    }
}
