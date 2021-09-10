package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrManager manager;
    private final GraphqlQueryToSelect graphqlQueryToSelect;

    public GraphqlMutationToStatements(GraphqlAntlrManager manager, GraphqlQueryToSelect graphqlQueryToSelect) {
        this.manager = manager;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
    }

    public List<String> createStatementsSql(String graphql) {
        return createSelectsSql(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    public List<String> createSelectsSql(GraphqlParser.DocumentContext documentContext) {
        return createStatements(documentContext).getStatements().stream()
                .map(Statement::toString).collect(Collectors.toList());
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
        if (operationDefinitionContext.operationType() != null && operationDefinitionContext.operationType().MUTATION() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = manager.getMutationOperationTypeDefinition();
            if (mutationOperationTypeDefinition.isPresent()) {
                Select select = new Select();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
                selectExpressionItem.setExpression(graphqlQueryToSelect.objectFieldSelectionToJsonObjectFunction(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()));
                body.setSelectItems(Collections.singletonList(selectExpressionItem));
                Table table = new Table("dual");
                body.setFromItem(table);
                select.setSelectBody(body);
                return Stream.concat(operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream), Stream.of(select));
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(mutationTypeName -> manager.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText()));
        if (mutationFieldTypeDefinitionContext.isPresent()) {
            return argumentsToStatementStream(mutationFieldTypeDefinitionContext.get(), selectionContext.field().arguments());
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarArgumentsToInsert(fieldDefinitionContext, argumentsContext);
        statementList.add(insert);

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName)));
        }

        fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(inputValueDefinitionContext.type().getText()))
                .forEach(inputValueDefinitionContext -> {

                    Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                    Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                    if (argumentContext.isPresent() && subFieldDefinitionContext.isPresent() && inputObjectTypeDefinition.isPresent()) {


                        statementList.addAll(singleTypeObjectValueWithVariableToStatementStream(subFieldDefinitionContext.get(), inputObjectTypeDefinition.get(), argumentContext.get().valueWithVariable().objectValueWithVariable()).collect(Collectors.toList()));

                        String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);
                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), argumentContext.get().valueWithVariable().objectValueWithVariable());


                        if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                            SubSelect subSelect = new SubSelect();
                            PlainSelect subBody = new PlainSelect();
                            String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(subFieldTypeName);
                            Table subTable = new Table(subTableName);
                            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
                            selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mappingToFieldDefinition.get().name().getText())));
                            selectExpressionItem.setAlias(new Alias(mappingToFieldDefinition.get().name().getText()));
                            subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
                            EqualsTo subEqualsTo = new EqualsTo();
                            subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(subIdFieldName.get())));
                            if (objectIdFieldWithVariableContext.isPresent()) {
                                subEqualsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectIdFieldWithVariableContext.get().valueWithVariable()));
                            } else {
                                subEqualsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get()));
                            }
                            subBody.setWhere(subEqualsTo);
                            subBody.setFromItem(subTable);
                            subSelect.setSelectBody(subBody);

                            Update update = new Update();
                            String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fieldTypeName);
                            Table table = new Table(tableName);
                            update.setTable(table);
                            Column updateColumn = new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mappingFromFieldDefinition.get().name().getText()));
                            update.setColumns(Collections.singletonList(updateColumn));
                            update.setExpressions(Collections.singletonList(subSelect));
                            EqualsTo equalsTo = new EqualsTo();
                            equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idFieldName.get())));
                            if (idArgumentContext.isPresent()) {
                                equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idArgumentContext.get().valueWithVariable()));
                            } else {
                                equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get()));
                            }
                            IsNullExpression isNullExpression = new IsNullExpression();
                            isNullExpression.setLeftExpression(updateColumn);
                            MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(isNullExpression, equalsTo));
                            update.setWhere(multiAndExpression);
                            statementList.add(update);
                        }


                    } else {
//                        return singleTypeDefaultValueToStatementStream(inputValueDefinitionContext.type(), inputValueDefinitionContext);
                    }


                });

//        Stream<Statement> stream3 = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
//                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
//                .flatMap(inputValueDefinitionContext -> listTypeArgumentToStatement(fieldDefinitionContext.type(), inputValueDefinitionContext, argumentsContext));

        return statementList.stream();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<Statement> statementList = new ArrayList<>();

        Statement insert = singleTypeScalarInputValuesToInsert(fieldDefinitionContext, inputObjectTypeDefinition, objectValueWithVariableContext);
        statementList.add(insert);

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());


        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariableContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName)));
        }

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(inputValueDefinitionContext.type().getText()))
                .forEach(inputValueDefinitionContext -> {

                    Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                    Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));


                    if (objectFieldWithVariableContext.isPresent() && subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {

                        statementList.addAll(singleTypeObjectValueWithVariableToStatementStream(subFieldDefinitionContext.get(), subInputObjectTypeDefinition.get(), objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable()).collect(Collectors.toList()));


                        String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);
                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());


                        if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                            SubSelect subSelect = new SubSelect();
                            PlainSelect subBody = new PlainSelect();
                            String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(subFieldTypeName);
                            Table subTable = new Table(subTableName);
                            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
                            selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mappingToFieldDefinition.get().name().getText())));
                            selectExpressionItem.setAlias(new Alias(mappingToFieldDefinition.get().name().getText()));
                            subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
                            EqualsTo subEqualsTo = new EqualsTo();
                            subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(subIdFieldName.get())));
                            if (objectIdFieldWithVariableContext.isPresent()) {
                                subEqualsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectIdFieldWithVariableContext.get().valueWithVariable()));
                            } else {
                                subEqualsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get()));
                            }
                            subBody.setWhere(subEqualsTo);
                            subBody.setFromItem(subTable);
                            subSelect.setSelectBody(subBody);

                            Update update = new Update();
                            String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fieldTypeName);
                            Table table = new Table(tableName);
                            update.setTable(table);
                            Column updateColumn = new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mappingFromFieldDefinition.get().name().getText()));
                            update.setColumns(Collections.singletonList(updateColumn));
                            update.setExpressions(Collections.singletonList(subSelect));
                            EqualsTo equalsTo = new EqualsTo();
                            equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idFieldName.get())));
                            if (idObjectFieldWithVariableContext.isPresent()) {
                                equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idObjectFieldWithVariableContext.get().valueWithVariable()));
                            } else {
                                equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get()));
                            }
                            IsNullExpression isNullExpression = new IsNullExpression();
                            isNullExpression.setLeftExpression(updateColumn);
                            MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(isNullExpression, equalsTo));
                            update.setWhere(multiAndExpression);
                            statementList.add(update);
                        }

                    }
                });

//        Stream<Statement> stream3 = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
//                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
//                .flatMap(inputValueDefinitionContext -> listTypeArgumentToStatement(fieldDefinitionContext.type(), inputValueDefinitionContext, argumentsContext));

        return statementList.stream();
    }

//    protected Stream<Statement> singleTypeArgumentToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
//
//        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
//        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(parentFieldDefinitionContext.type(), inputValueDefinitionContext);
//        Optional<GraphqlParser.ArgumentContext> parentIdArgumentContext = manager.getIDArgument(parentFieldDefinitionContext.type(), argumentsContext);
//
//        if (argumentContext.isPresent() && fieldDefinitionContext.isPresent()) {
//            return singleTypeObjectValueWithVariableToStatementStream(parentFieldDefinitionContext, parentIdArgumentContext.orElse(null), fieldDefinitionContext.get(), inputValueDefinitionContext, argumentContext.get());
//        } else {
//            return singleTypeDefaultValueToStatementStream(inputValueDefinitionContext.type(), inputValueDefinitionContext);
//        }
//    }
//
//    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext,
//                                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
//                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
//
//        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
//        if (objectFieldWithVariableContext.isPresent()) {
//            if (objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable() != null) {
//                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                if (fieldDefinitionContext.isPresent()) {
//                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = manager.getIDObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
//                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idObjectFieldWithVariable.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
//                }
//            }
//        } else {
//            return singleTypeDefaultValueToStatementStream(typeContext, inputValueDefinitionContext);
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
//
//        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
//        if (objectFieldContext.isPresent()) {
//            if (objectFieldContext.get().value().objectValue() != null) {
//                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                if (fieldDefinitionContext.isPresent()) {
//                    Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(typeContext, objectValueContext);
//                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldContext.get().value().objectValue());
//                }
//            }
//        } else {
//            return singleTypeDefaultValueToStatementStream(typeContext, inputValueDefinitionContext);
//        }
//        return Stream.empty();
//    }
//
//    protected Stream<Statement> singleTypeDefaultValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
//
//        if (inputValueDefinitionContext.type().nonNullType() != null) {
//            if (inputValueDefinitionContext.defaultValue() != null) {
//                Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(inputValueDefinitionContext.defaultValue().value().objectValue(), inputValueDefinitionContext);
//                if (objectFieldContext.isPresent()) {
//                    Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                    if (fieldDefinitionContext.isPresent()) {
//                        Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(typeContext, inputValueDefinitionContext.defaultValue().value().objectValue());
//                        return singleTypeObjectValueToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldContext.get().value().objectValue());
//                    }
//                }
//            } else {
//                //todo
//            }
//        }
//        return Stream.empty();
//    }
//
//    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
//                                                                                   GraphqlParser.ArgumentContext parentIdArgumentContext,
//                                                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
//                                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
//                                                                                   GraphqlParser.ArgumentContext argumentContext) {
//
//        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
//
//
//        if (inputObjectTypeDefinition.isPresent()) {
//
//            return Stream.concat(
//                    Stream.concat(
//                            Stream.concat(singleTypeScalarInputValuesToStatementStream(fieldDefinitionContext, argumentContext),
//                                    Stream.of(parentTypeRelationFieldUpdate(parentFieldDefinitionContext, parentIdArgumentContext, fieldDefinitionContext, inputObjectTypeDefinition.get(), argumentContext))),
//                            inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                                    .filter(fieldInputValueDefinitionContext -> manager.isInputObject(fieldInputValueDefinitionContext.type().getText()))
//                                    .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(parentFieldDefinitionContext, parentIdArgumentContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
//                    ),
//                    inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                            .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                            .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
//            );
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext,
//                                                                                   GraphqlParser.ValueContext parentTypeIdValueContext,
//                                                                                   GraphqlParser.TypeContext typeContext,
//                                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
//                                                                                   GraphqlParser.ObjectValueContext objectValueContext) {
//
//
//        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
//        if (inputObjectTypeDefinition.isPresent()) {
//            List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                    .filter(fieldInputValueDefinitionContext -> manager.isScaLar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());
//
//            return Stream.concat(
//                    Stream.concat(
//                            Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueContext),
//                                    Stream.of(parentTypeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
//                            inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                                    .filter(fieldInputValueDefinitionContext -> manager.isInputObject(fieldInputValueDefinitionContext.type().getText()))
//                                    .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//                    ),
//                    inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                            .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                            .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//            );
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentTypeIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
//
//        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
//        if (inputObjectTypeDefinition.isPresent()) {
//            List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                    .filter(fieldInputValueDefinitionContext -> manager.isScaLar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());
//
//            return Stream.concat(
//                    Stream.concat(
//                            Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueContext),
//                                    Stream.of(parentTypeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
//                            inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                                    .filter(fieldInputValueDefinitionContext -> manager.isInputObject(fieldInputValueDefinitionContext.type().getText()))
//                                    .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//                    ),
//                    inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                            .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                            .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//            );
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> listTypeArgumentToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
//
//        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
//        if (argumentContext.isPresent()) {
//            if (argumentContext.get().valueWithVariable().arrayValueWithVariable() != null) {
//                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                if (fieldDefinitionContext.isPresent()) {
//                    Optional<GraphqlParser.ArgumentContext> idArgument = manager.getIDArgument(typeContext, argumentsContext);
//                    return Stream.concat(argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
//                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
//                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
//                            Stream.of(listTypeFieldDelete(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
//                                    argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
//                                            .map(valueWithVariableContext -> manager.getIDObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
//                                            .filter(Optional::isPresent)
//                                            .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.get().valueWithVariable())
//                                            .collect(Collectors.toList())))
//                    );
//                }
//            }
//        } else {
//            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
//
//        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
//        if (objectFieldWithVariableContext.isPresent()) {
//            if (objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable() != null) {
//                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                if (fieldDefinitionContext.isPresent()) {
//                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = manager.getIDObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
//                    return Stream.concat(objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
//                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
//                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
//                            Stream.of(listTypeFieldDelete(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
//                                    objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
//                                            .map(valueWithVariableContext -> manager.getIDObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
//                                            .filter(Optional::isPresent)
//                                            .map(updateObjectFieldWithVariableContext -> updateObjectFieldWithVariableContext.get().valueWithVariable())
//                                            .collect(Collectors.toList())))
//                    );
//                }
//            }
//        } else {
//            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> listTypeObjectValueToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
//
//        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
//        if (objectFieldContext.isPresent()) {
//            if (objectFieldContext.get().value().arrayValue() != null) {
//                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                if (fieldDefinitionContext.isPresent()) {
//                    Optional<GraphqlParser.ObjectFieldContext> idField = manager.getIDObjectField(typeContext, objectValueContext);
//                    return Stream.concat(objectFieldContext.get().value().arrayValue().value().stream()
//                                    .filter(valueContext -> valueContext.objectValue() != null)
//                                    .flatMap(valueContext -> listTypeObjectValueToStatementStream(typeContext, idField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueContext.objectValue())),
//                            Stream.of(listTypeFieldDelete(typeContext, idField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(),
//                                    objectFieldContext.get().value().arrayValue().value().stream()
//                                            .map(valueContext -> manager.getIDObjectField(typeContext, valueContext.objectValue()))
//                                            .filter(Optional::isPresent)
//                                            .map(updateObjectFieldContext -> updateObjectFieldContext.get().value())
//                                            .collect(Collectors.toList())))
//                    );
//                }
//            }
//        } else {
//            return listTypeDefaultValueToStatement(typeContext, inputValueDefinitionContext);
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> listTypeDefaultValueToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
//
//        if (inputValueDefinitionContext.type().nonNullType() != null) {
//            if (inputValueDefinitionContext.defaultValue() != null) {
//                Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(inputValueDefinitionContext.defaultValue().value().objectValue(), inputValueDefinitionContext);
//                if (objectFieldContext.isPresent()) {
//                    Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
//                    if (fieldDefinitionContext.isPresent()) {
//                        Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(typeContext, inputValueDefinitionContext.defaultValue().value().objectValue());
//                        return Stream.concat(inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
//                                        .filter(valueContext -> valueContext.objectValue() != null)
//                                        .flatMap(valueContext -> listTypeObjectValueToStatementStream(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueContext.objectValue())),
//                                Stream.of(listTypeFieldDelete(typeContext, idObjectField.map(GraphqlParser.ObjectFieldContext::value).orElse(null), fieldDefinitionContext.get().type(),
//                                        inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
//                                                .map(valueContext -> manager.getIDObjectField(typeContext, valueContext.objectValue()))
//                                                .filter(Optional::isPresent)
//                                                .map(updateObjectFieldContext -> updateObjectFieldContext.get().value())
//                                                .collect(Collectors.toList())))
//                        );
//                    }
//                }
//            } else {
//                //todo
//            }
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentTypeIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
//
//        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
//        if (inputObjectTypeDefinition.isPresent()) {
//            List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                    .filter(fieldInputValueDefinitionContext -> manager.isScaLar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());
//
//            return Stream.concat(
//                    Stream.concat(
//                            Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext),
//                                    Stream.of(typeRelationFieldUpdate(parentTypeContext, parentTypeIdValueWithVariableContext, typeContext, objectValueWithVariableContext))),
//                            inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                                    .filter(fieldInputValueDefinitionContext -> manager.isInputObject(fieldInputValueDefinitionContext.type().getText()))
//                                    .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
//                    ),
//                    inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                            .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                            .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
//            );
//        }
//        return Stream.empty();
//    }


//    protected Stream<Statement> listTypeObjectValueToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentTypeIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
//
//        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
//        if (inputObjectTypeDefinition.isPresent()) {
//            List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                    .filter(fieldInputValueDefinitionContext -> manager.isScaLar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());
//
//            return Stream.concat(
//                    Stream.concat(
//                            Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, scalarInputValueDefinitionContextList, objectValueContext),
//                                    Stream.of(typeRelationFieldUpdate(parentTypeContext, parentTypeIdValueContext, typeContext, objectValueContext))),
//                            inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                                    .filter(fieldInputValueDefinitionContext -> !manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                                    .filter(fieldInputValueDefinitionContext -> manager.isInputObject(fieldInputValueDefinitionContext.type().getText()))
//                                    .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//                    ),
//                    inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
//                            .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
//                            .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueContext))
//            );
//        }
//        return Stream.empty();
//    }

//    protected Stream<Statement> singleTypeScalarArgumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
//        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
//        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
//        return Stream.of(singleTypeScalarArgumentsToInsert(fieldDefinitionContext, argumentsContext), DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName.orElse(null)));
//    }

    protected Insert singleTypeScalarArgumentsToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isScaLar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(fieldDefinitionContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        insert.setUseDuplicate(true);
        insert.setDuplicateUpdateColumns(columnList);
        insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList = inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isScaLar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(fieldDefinitionContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        insert.setUseDuplicate(true);
        insert.setDuplicateUpdateColumns(columnList);
        insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }


    protected Optional<Column> singleTypeScalarArgumentsToColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
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

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
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

    protected Optional<Expression> singleTypeScalarArgumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

//    protected Stream<Statement> singleTypeScalarInputValuesToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
//        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
//        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
//        return Stream.of(singleTypeScalarInputValuesToInsert(fieldDefinitionContext, argumentContext), DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName.orElse(null)));
//    }

//    protected Stream<Statement> singleTypeScalarInputValuesToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {
//        Optional<GraphqlParser.ObjectFieldContext> idField = manager.getIDObjectField(typeContext, objectValueContext);
//        String fieldTypeName = manager.getFieldTypeName(typeContext);
//        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
//        return idField.<Stream<Statement>>map(objectFieldContext -> Stream.of(singleTypeScalarInputValuesToUpdate(typeContext, objectFieldContext, inputValueDefinitionContextList, objectValueContext)))
//                .orElseGet(() -> Stream.of(singleTypeScalarInputValuesToInsert(typeContext, inputValueDefinitionContextList, objectValueContext), DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName.orElse(null))));
//    }

//    protected Update singleTypeScalarInputValuesToUpdate(GraphqlParser.TypeContext typeContext,
//                                                         GraphqlParser.ObjectFieldWithVariableContext idField,
//                                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
//                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
//        Update update = new Update();
//        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
//        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        update.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idField.name().getText())));
//        equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
//        update.setWhere(equalsTo);
//        return update;
//    }

//    protected Update singleTypeScalarInputValuesToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {
//        Update update = new Update();
//        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
//        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        update.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idField.name().getText())));
//        equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(idField.value()));
//        update.setWhere(equalsTo);
//        return update;
//    }
//
//    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueContext objectValueContext) {
//        Insert insert = new Insert();
//        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(typeContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
//        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        insert.setTable(table);
//        return insert;
//    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
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
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.get().value()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

//    protected Update parentTypeRelationFieldUpdate(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
//                                                   GraphqlParser.ArgumentContext parentIdArgumentContext,
//                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
//                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
//                                                   GraphqlParser.ArgumentContext argumentContext) {
//
//        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
//        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(parentFieldTypeName);
//        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fieldTypeName);
//
//        Table table = new Table(tableName);
//        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);
//        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(fieldDefinitionContext);
//
//        SubSelect subSelect = new SubSelect();
//        PlainSelect body = new PlainSelect();
//        Table subTable = new Table(subTableName);
//        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
//        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mappingToFieldDefinition.get().name().getText())));
//        selectExpressionItem.setAlias(new Alias(mappingToFieldDefinition.get().name().getText()));
//        body.setSelectItems(Collections.singletonList(selectExpressionItem));
//        EqualsTo equalsTo = new EqualsTo();
//        manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> equalsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(idFieldName))));
//        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), argumentContext.valueWithVariable().objectValueWithVariable());
//        if (fieldIdField.isPresent()) {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable()));
//        } else {
//            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName)));
//        }
//        body.setWhere(equalsTo);
//        body.setFromItem(subTable);
//        subSelect.setSelectBody(body);
//
//        Update update = new Update();
//        update.setTable(table);
//
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(mappingFromFieldDefinition.get().name().getText()).orElse(null))));
//        if (parentIdArgumentContext.isPresent()) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        } else {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext));
//        }
//        update.setWhere(equalsTo);
//        return update;
//    }

//    protected Update typeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        Update update = new Update();
//        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(manager.getFieldTypeName(typeContext), manager.getFieldTypeName(parentTypeContext)).orElse(null)))));
//        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = manager.getIDObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
//        if (parentIdValueWithVariableContext == null) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            update.setExpressions(Collections.singletonList(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null))));
//        } else {
//            update.setExpressions(Collections.singletonList(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext)));
//        }
//        update.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(parentTypeContext)).orElse(null))));
//        if (fieldIdField.isPresent()) {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(fieldIdField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null)));
//        } else {
//            String fieldTypeName = manager.getFieldTypeName(typeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        }
//        update.setWhere(equalsTo);
//        return update;
//    }

//    protected Delete listTypeFieldDelete(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext typeContext, List<GraphqlParser.ValueWithVariableContext> idValueWithVariableContextList) {
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        Delete delete = new Delete();
//        delete.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(manager.getFieldTypeName(typeContext), manager.getFieldTypeName(parentTypeContext)).orElse(null))));
//        if (idValueWithVariableContext == null) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        } else {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idValueWithVariableContext));
//        }
//        InExpression inExpression = new InExpression();
//        inExpression.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(typeContext)).orElse(null))));
//        inExpression.setNot(true);
//        inExpression.setRightItemsList(new ExpressionList(idValueWithVariableContextList.stream().map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
//        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
//        return delete;
//    }

//    protected Update parentTypeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(parentTypeContext));
//        Table table = new Table(tableName);
//        Update update = new Update();
//        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(manager.getFieldTypeName(parentTypeContext), manager.getFieldTypeName(typeContext)).orElse(null)))));
//        Optional<GraphqlParser.ObjectFieldContext> fieldIdField = manager.getIDObjectField(typeContext, objectValueContext);
//        if (fieldIdField.isPresent()) {
//            update.setExpressions(Collections.singletonList(fieldIdField.map(field -> DB_VALUE_UTIL.scalarValueToDBValue(field.value())).orElse(null)));
//        } else {
//            String fieldTypeName = manager.getFieldTypeName(typeContext);
//            update.setExpressions(Collections.singletonList(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null))));
//        }
//        update.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(parentTypeContext)).orElse(null))));
//        if (parentIdValueContext == null) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        } else {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(parentIdValueContext));
//        }
//        update.setWhere(equalsTo);
//        return update;
//    }

//    protected Update typeRelationFieldUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext parentIdValueContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueContext objectValueContext) {
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        Update update = new Update();
//        update.setColumns(Collections.singletonList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(manager.getFieldTypeName(typeContext), manager.getFieldTypeName(parentTypeContext)).orElse(null)))));
//        Optional<GraphqlParser.ObjectFieldContext> fieldIdField = manager.getIDObjectField(typeContext, objectValueContext);
//        if (parentIdValueContext == null) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            update.setExpressions(Collections.singletonList(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null))));
//        } else {
//            update.setExpressions(Collections.singletonList(DB_VALUE_UTIL.scalarValueToDBValue(parentIdValueContext)));
//        }
//        update.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(parentTypeContext)).orElse(null))));
//        if (fieldIdField.isPresent()) {
//            equalsTo.setRightExpression(fieldIdField.map(field -> DB_VALUE_UTIL.scalarValueToDBValue(field.value())).orElse(null));
//        } else {
//            String fieldTypeName = manager.getFieldTypeName(typeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        }
//        update.setWhere(equalsTo);
//        return update;
//    }

//    protected Delete listTypeFieldDelete(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueContext idValueContext, GraphqlParser.TypeContext typeContext, List<GraphqlParser.ValueContext> idValueContextList) {
//        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
//        Table table = new Table(tableName);
//        Delete delete = new Delete();
//        delete.setTable(table);
//        EqualsTo equalsTo = new EqualsTo();
//        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(manager.getFieldTypeName(typeContext), manager.getFieldTypeName(parentTypeContext)).orElse(null))));
//        if (idValueContext == null) {
//            String fieldTypeName = manager.getFieldTypeName(parentTypeContext);
//            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName).orElse(null)));
//        } else {
//            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueToDBValue(idValueContext));
//        }
//        InExpression inExpression = new InExpression();
//        inExpression.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(typeContext)).orElse(null))));
//        inExpression.setNot(true);
//        inExpression.setRightItemsList(new ExpressionList(idValueContextList.stream().map(DB_VALUE_UTIL::scalarValueToDBValue).collect(Collectors.toList())));
//        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
//        return delete;
//    }

    protected String argumentToColumnSpecs(GraphqlParser.ArgumentContext argumentContext) {
        if (argumentContext.valueWithVariable().IntValue() != null) {
            return DB_NAME_UTIL.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().IntValue().getText());
        } else if (argumentContext.valueWithVariable().BooleanValue() != null) {
            return DB_NAME_UTIL.booleanDirectiveTocColumnDefinition(argumentContext.name().getText());
        } else if (argumentContext.valueWithVariable().StringValue() != null) {
            return DB_NAME_UTIL.directiveTocColumnDefinition(argumentContext.name().getText(), argumentContext.valueWithVariable().StringValue().getText());
        }
        //TODO
        return null;
    }
}
