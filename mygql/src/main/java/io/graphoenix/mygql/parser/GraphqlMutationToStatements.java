package io.graphoenix.mygql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.mygql.common.utils.DBNameUtil.DB_NAME_UTIL;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrRegister register;
    private final GraphqlQueryToSelect graphqlQueryToSelect;

    public GraphqlMutationToStatements(GraphqlAntlrRegister register, GraphqlQueryToSelect graphqlQueryToSelect) {
        this.register = register;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
    }

    public Statements createStatements(GraphqlParser.DocumentContext documentContext) {
        Statements statements = new Statements();
        statements.setStatements(documentContext.definition().stream().flatMap(this::createStatementStream).collect(Collectors.toList()));
        return statements;
    }

    protected Stream<Statement> createStatementStream(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() != null) {
            return operationDefinitionToStatementStream(definitionContext.operationDefinition());
        }
        return Stream.empty();
    }

    protected Stream<Statement> operationDefinitionToStatementStream(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() != null && operationDefinitionContext.operationType().getText().equals("mutation")) {

            Select select = new Select();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(objectFieldSelectionToJsonObjectFunction(null, operationDefinitionContext.selectionSet().selection()));
            body.setSelectItems(Collections.singletonList(selectExpressionItem));

            Table table = new Table("dual");
            body.setFromItem(table);
            select.setSelectBody(body);
            return Stream.concat(operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream), Stream.of(select));
        }
        return Stream.empty();
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        String mutationTypeName = register.getMutationTypeName();
        Optional<GraphqlParser.TypeContext> mutationFieldTypeContext = register.getObjectFieldTypeContext(mutationTypeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = register.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText());

        if (mutationFieldTypeContext.isPresent()) {
            if (mutationFieldTypeDefinitionContext.isPresent()) {
                return argumentsToStatementStream(mutationFieldTypeContext.get(), mutationFieldTypeDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
            }
        }
        return Stream.empty();
    }

    protected Function objectFieldSelectionToJsonObjectFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContexts.stream()
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), mutationSelectionToExpression(typeContext, selectionContext)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));

        return function;
    }

    protected SubSelect mutationSelectionToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? register.getQueryTypeName() : register.getFieldTypeName(typeContext);
        String filedTypeName = register.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());
        if (register.isObject(filedTypeName)) {
            return graphqlQueryToSelect.objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext, true);
        }
        return null;
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> register.isInnerScalar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        singleTypeScalarArgumentsToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, argumentsContext),
                        argumentsDefinitionContext.inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                                .filter(inputValueDefinitionContext -> register.isInputObject(inputValueDefinitionContext.type().getText()))
                                .flatMap(inputValueDefinitionContext -> singleTypeArgumentToStatementStream(typeContext, inputValueDefinitionContext, argumentsContext))
                ),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext -> register.fieldTypeIsList(inputValueDefinitionContext.type()))
                        .flatMap(inputValueDefinitionContext -> listTypeArgumentToStatement(typeContext, inputValueDefinitionContext, argumentsContext))
        );
    }

    protected Stream<Statement> singleTypeArgumentToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().objectValueWithVariable() != null) {
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ArgumentContext> idArgument = register.getIdArgument(typeContext, argumentsContext);
                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, argumentContext.get().valueWithVariable().objectValueWithVariable());
                }
            }
        } else {
            return singleTypeDefaultValueToStatementStream(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeDefaultValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {

        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(inputValueDefinitionContext.defaultValue().value().objectValue(), inputValueDefinitionContext);
                if (objectFieldContext.isPresent()) {
                    Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                    if (fieldDefinitionContext.isPresent()) {
                        Optional<GraphqlParser.ObjectFieldContext> idObjectField = register.getIdObjectField(typeContext, inputValueDefinitionContext.defaultValue().value().objectValue());
                        return singleTypeObjectValueToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldContext.get().value().objectValue());
                    }
                }
            } else {
                //todo
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable() != null) {
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = register.getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idObjectFieldWithVariable.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                }
            }
        } else {
            return singleTypeDefaultValueToStatementStream(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {

        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            if (objectFieldContext.get().value().objectValue() != null) {
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldContext> idObjectField = register.getIdObjectField(typeContext, objectValueContext);
                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldContext.get().value().objectValue());
                }
            }
        } else {
            return singleTypeDefaultValueToStatementStream(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentTypeIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext);
        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(parentTypeRelationFieldUpdate(parentTypeContext, parentTypeIdValueWithVariableContext, typeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
        );
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentTypeIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext);
        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueContext),
                                Stream.of(parentTypeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
        );
    }

    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentTypeIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getFieldTypeDefinition(inputValueDefinitionContext);
        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueContext),
                                Stream.of(parentTypeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
        );
    }

    protected Stream<Statement> listTypeArgumentToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ArgumentContext> idArgument = register.getIdArgument(typeContext, argumentsContext);
                    return Stream.concat(argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                            Stream.of(listTypeFieldDelete(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
                                    argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> register.getIdObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
                                            .filter(Optional::isPresent)
                                            .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.get().valueWithVariable())
                                            .collect(Collectors.toList())))
                    );
                }
            }
        } else {
            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeDefaultValueToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {

        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(inputValueDefinitionContext.defaultValue().value().objectValue(), inputValueDefinitionContext);
                if (objectFieldContext.isPresent()) {
                    Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                    if (fieldDefinitionContext.isPresent()) {
                        Optional<GraphqlParser.ObjectFieldContext> idObjectField = register.getIdObjectField(typeContext, inputValueDefinitionContext.defaultValue().value().objectValue());
                        return Stream.concat(inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
                                        .filter(valueContext -> valueContext.objectValue() != null)
                                        .flatMap(valueContext -> listTypeObjectValueToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueContext.objectValue())),
                                Stream.of(listTypeFieldDelete(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(),
                                        inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
                                                .map(valueContext -> register.getIdObjectField(typeContext, valueContext.objectValue()))
                                                .filter(Optional::isPresent)
                                                .map(updateObjectFieldContext -> updateObjectFieldContext.get().value())
                                                .collect(Collectors.toList())))
                        );
                    }
                }
            } else {
                //todo
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = register.getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
                    return Stream.concat(objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                            Stream.of(listTypeFieldDelete(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
                                    objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> register.getIdObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
                                            .filter(Optional::isPresent)
                                            .map(updateObjectFieldWithVariableContext -> updateObjectFieldWithVariableContext.get().valueWithVariable())
                                            .collect(Collectors.toList())))
                    );
                }
            }
        } else {
            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeObjectValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {

        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            if (objectFieldContext.get().value().arrayValue() != null) {
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getTypeFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldContext> idField = register.getIdObjectField(typeContext, objectValueContext);
                    return Stream.concat(objectFieldContext.get().value().arrayValue().value().stream()
                                    .filter(valueContext -> valueContext.objectValue() != null)
                                    .flatMap(valueContext -> listTypeObjectValueToStatementStream(typeContext, idField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueContext.objectValue())),
                            Stream.of(listTypeFieldDelete(typeContext, idField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(),
                                    objectFieldContext.get().value().arrayValue().value().stream()
                                            .map(valueContext -> register.getIdObjectField(typeContext, valueContext.objectValue()))
                                            .filter(Optional::isPresent)
                                            .map(updateObjectFieldContext -> updateObjectFieldContext.get().value())
                                            .collect(Collectors.toList())))
                    );
                }
            }
        } else {
            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentTypeIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(typeRelationFieldUpdate(parentTypeContext, parentTypeIdValueWithVariableContext, typeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
        );
    }


    protected Stream<Statement> listTypeObjectValueToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentTypeIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, scalarInputValueDefinitionContextList, objectValueContext),
                                Stream.of(typeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
        );
    }

    protected Stream<Statement> singleTypeScalarArgumentsToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> idField = register.getIdArgument(typeContext, argumentsContext);
        return idField.<Stream<Statement>>map(argumentContext -> Stream.of(singleTypeScalarArgumentsToUpdate(typeContext, argumentContext, inputValueDefinitionContextList, argumentsContext))).orElseGet(() -> Stream.of(singleTypeScalarArgumentsToInsert(typeContext, inputValueDefinitionContextList, argumentsContext), register.createInsertIdSetStatement(typeContext)));
    }

    protected Update singleTypeScalarArgumentsToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(typeContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert singleTypeScalarArgumentsToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(typeContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> singleTypeScalarArgumentsToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        if (argumentContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(argumentContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarArgumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return Optional.of(register.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Stream<Statement> singleTypeScalarInputValuesToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = register.getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        return idField.<Stream<Statement>>map(objectFieldWithVariableContext -> Stream.of(singleTypeScalarInputValuesToUpdate(typeContext, objectFieldWithVariableContext, inputValueDefinitionContextList, objectValueWithVariableContext))).orElseGet(() -> Stream.of(singleTypeScalarInputValuesToInsert(typeContext, inputValueDefinitionContextList, objectValueWithVariableContext), register.createInsertIdSetStatement(typeContext)));
    }

    protected Stream<Statement> singleTypeScalarInputValuesToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {

        Optional<GraphqlParser.ObjectFieldContext> idField = register.getIdObjectField(typeContext, objectValueContext);
        return idField.<Stream<Statement>>map(objectFieldContext -> Stream.of(singleTypeScalarInputValuesToUpdate(typeContext, objectFieldContext, inputValueDefinitionContextList, objectValueContext))).orElseGet(() -> Stream.of(singleTypeScalarInputValuesToInsert(typeContext, inputValueDefinitionContextList, objectValueContext), register.createInsertIdSetStatement(typeContext)));
    }

    protected Update singleTypeScalarInputValuesToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Update singleTypeScalarInputValuesToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueToDBValue(idField.value()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldWithVariableContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        if (objectFieldContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(register.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = register.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return Optional.of(register.scalarValueToDBValue(objectFieldContext.get().value()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Update parentTypeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(parentTypeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(parentTypeContext), register.getFieldTypeName(typeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = register.getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        if (fieldIdField.isPresent()) {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable())));
        } else {
            update.setExpressions(Collections.singletonList(register.createInsertIdUserVariable(typeContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (parentIdValueWithVariableContext == null) {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(parentTypeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Update typeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(typeContext), register.getFieldTypeName(parentTypeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = register.getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        if (parentIdValueWithVariableContext == null) {
            update.setExpressions(Collections.singletonList(register.createInsertIdUserVariable(parentTypeContext)));
        } else {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (fieldIdField.isPresent()) {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable()));
        } else {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(typeContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Delete listTypeFieldDelete(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext typeContext, List<GraphqlParser.ValueWithVariableContext> idValueWithVariableContextList) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (idValueWithVariableContext == null) {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(typeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idValueWithVariableContext));
        }
        InExpression inExpression = new InExpression();
        inExpression.setNot(true);
        inExpression.setRightItemsList(new ExpressionList(idValueWithVariableContextList.stream().map(register::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        return delete;
    }

    protected Update parentTypeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(parentTypeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(parentTypeContext), register.getFieldTypeName(typeContext))))));
        Optional<GraphqlParser.ObjectFieldContext> fieldIdField = register.getIdObjectField(typeContext, objectValueContext);
        if (fieldIdField.isPresent()) {
            update.setExpressions(Collections.singletonList(register.scalarValueToDBValue(fieldIdField.get().value())));
        } else {
            update.setExpressions(Collections.singletonList(register.createInsertIdUserVariable(typeContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (parentIdValueContext == null) {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(parentTypeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueToDBValue(parentIdValueContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Update typeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(typeContext), register.getFieldTypeName(parentTypeContext))))));
        Optional<GraphqlParser.ObjectFieldContext> fieldIdField = register.getIdObjectField(typeContext, objectValueContext);
        if (parentIdValueContext == null) {
            update.setExpressions(Collections.singletonList(register.createInsertIdUserVariable(parentTypeContext)));
        } else {
            update.setExpressions(Collections.singletonList(register.scalarValueToDBValue(parentIdValueContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (fieldIdField.isPresent()) {
            equalsTo.setRightExpression(register.scalarValueToDBValue(fieldIdField.get().value()));
        } else {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(typeContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Delete listTypeFieldDelete(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext idValueContext, GraphqlParser.TypeContext typeContext, List<GraphqlParser.ValueContext> idValueContextList) {
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (idValueContext == null) {
            equalsTo.setRightExpression(register.createInsertIdUserVariable(typeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueToDBValue(idValueContext));
        }
        InExpression inExpression = new InExpression();
        inExpression.setNot(true);
        inExpression.setRightItemsList(new ExpressionList(idValueContextList.stream().map(register::scalarValueToDBValue).collect(Collectors.toList())));
        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        return delete;
    }
}
