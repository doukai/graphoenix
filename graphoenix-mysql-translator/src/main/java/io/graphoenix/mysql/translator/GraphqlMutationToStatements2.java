package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
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

public class GraphqlMutationToStatements2 {

    private final GraphqlAntlrManager manager;
    private final GraphqlQueryToSelect graphqlQueryToSelect;

    public GraphqlMutationToStatements2(GraphqlAntlrManager manager, GraphqlQueryToSelect graphqlQueryToSelect) {
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
//                selectExpressionItem.setExpression(graphqlQueryToSelect.objectFieldSelectionToJsonObjectFunction(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()));
                body.setSelectItems(Collections.singletonList(selectExpressionItem));
                Table table = new Table("dual");
                body.setFromItem(table);
                select.setSelectBody(body);
                return Stream.concat(operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream), Stream.of(select));
            }
        }
        return Stream.empty();
    }

    protected List<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        List<Statement> statementList = new ArrayList<>();
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(mutationTypeName -> manager.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText()));

        if (mutationFieldTypeDefinitionContext.isPresent()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = mutationFieldTypeDefinitionContext.get();
            GraphqlParser.ArgumentsContext argumentsContext = selectionContext.field().arguments();

            statementList.addAll(argumentsToStatementList(fieldDefinitionContext, argumentsContext));

            statementList.addAll(
                    fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                            .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                            .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                            .map(inputValueDefinitionContext ->
                                    manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                            .map(subFieldDefinitionContext ->
                                                    manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext)
                                                            .map(argumentContext ->
                                                                    objectTypeArgumentToStatementList(
                                                                            fieldDefinitionContext,
                                                                            subFieldDefinitionContext,
                                                                            inputValueDefinitionContext,
                                                                            argumentContext,
                                                                            0,
                                                                            0
                                                                    )
                                                            ).orElse(
                                                            objectTypeInputValueToStatementList(
                                                                    fieldDefinitionContext,
                                                                    subFieldDefinitionContext,
                                                                    inputValueDefinitionContext,
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                            ).filter(Optional::isPresent)
                            .flatMap(list -> list.get().stream())
                            .collect(Collectors.toList())
            );
        }
        return statementList;
    }

    protected List<Statement> argumentsToStatementList(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                       GraphqlParser.ArgumentsContext argumentsContext) {
        List<Statement> statementList = new ArrayList<>();
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        statementList.add(argumentsToInsert(table, fieldList, argumentsContext));
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty()) {
            idFieldName.ifPresent(name -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(typeName, name, 0, 0)));
        }

        return statementList;
    }


    protected List<Statement> objectTypeArgumentToStatementList(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                GraphqlParser.ArgumentContext argumentContext,
                                                                int level,
                                                                int index) {

        List<Statement> statementList = new ArrayList<>();
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        GraphqlParser.ObjectValueWithVariableContext objectValueWithVariable = argumentContext.valueWithVariable().objectValueWithVariable();

        statementList.addAll(objectValueWithVariableToStatementList(fieldDefinitionContext, objectValueWithVariable, level + 1, index));

        if (inputObjectTypeDefinition.isPresent()) {

            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, fieldDefinitionContext);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);
            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

        }

        return statementList;
    }

    protected List<Statement> objectValueWithVariableToStatementList(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                     GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                     int level,
                                                                     int index) {
        List<Statement> statementList = new ArrayList<>();
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        statementList.add(objectValueToInsert(table, fieldList, objectValueWithVariableContext));
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariable.isEmpty()) {
            idFieldName.ifPresent(name -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(typeName, name, level, index)));
        }

        return statementList;
    }

    protected List<Statement> objectTypeInputValueToStatementList(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                  GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                  GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                  int level,
                                                                  int index) {

    }


    protected Insert argumentsToInsert(Table table,
                                       List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                       GraphqlParser.ArgumentsContext argumentsContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                .map(argumentContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), argumentContext))
                                .orElse(fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                ).collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                        .map(argumentContext -> argumentToDBValue(inputValueDefinitionContext, argumentContext))
                                        .orElse(argumentToDBValue(inputValueDefinitionContext))
                        ).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }


    protected Insert objectValueToInsert(Table table,
                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                .map(objectFieldWithVariableContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), objectFieldWithVariableContext))
                                .orElse(fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                ).collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                        .map(objectFieldWithVariableContext -> objectFieldWithVariableToDBValue(inputValueDefinitionContext, objectFieldWithVariableContext))
                                        .orElse(argumentToDBValue(inputValueDefinitionContext))
                        ).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }


    protected Insert defaultValueToInsert(Table table,
                                          List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.defaultValue() != null)
                .map(inputValueDefinitionContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .filter(inputValueDefinitionContext -> inputValueDefinitionContext.defaultValue() != null)
                        .map(this::defaultValueToDBValue).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }

    protected Expression argumentToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(argumentContext.valueWithVariable());
        }
        return null;
    }

    protected Expression objectFieldWithVariableToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        }
        return null;
    }

    protected Expression defaultValueToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueToDBValue(inputValueDefinitionContext.defaultValue().value());
        }
        return null;
    }

    protected Expression argumentToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.enumValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                }
            } else {
                //TODO
            }
        }
        return null;
    }

    protected Table typeToTable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return typeToTable(fieldDefinitionContext.type());
    }

    protected Table typeToTable(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return typeToTable(inputValueDefinitionContext.type());
    }

    protected Table typeToTable(GraphqlParser.TypeContext typeContext) {
        return new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext)));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ArgumentContext argumentContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    protected Expression createIdValueExpression(GraphqlParser.ArgumentContext idArgumentContext) {
        return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idArgumentContext.valueWithVariable());
    }

    protected Expression createIdValueExpression(GraphqlParser.ObjectFieldWithVariableContext objectIdFieldWithVariableContext) {
        return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectIdFieldWithVariableContext.valueWithVariable());
    }

    protected Expression createIdValueExpression(GraphqlParser.ObjectFieldContext objectIdFieldContext) {
        return DB_VALUE_UTIL.scalarValueToDBValue(objectIdFieldContext.value());
    }

    protected Insert insertExpression(Table table,
                                      List<Column> columnList,
                                      ExpressionList expressionList) {
        return insertExpression(table, columnList, expressionList, false);
    }

    protected Insert insertExpression(Table table,
                                      List<Column> columnList,
                                      ExpressionList expressionList,
                                      boolean useDuplicate) {

        Insert insert = new Insert();
        insert.setTable(table);
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        if (useDuplicate && columnList.size() > 0) {
            insert.setUseDuplicate(true);
            insert.setDuplicateUpdateColumns(columnList);
            insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        }
        return insert;
    }


    protected Delete deleteExpression(Table table, Expression where) {

        Delete delete = new Delete();
        delete.setTable(table);
        delete.setWhere(where);
        return delete;
    }


    protected SubSelect selectFieldByIdExpression(Table table,
                                                  String fieldName,
                                                  String idFieldName,
                                                  Expression idFieldValueExpression) {


        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        subBody.setFromItem(table);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(idFieldName)));
        subEqualsTo.setRightExpression(idFieldValueExpression);
        subBody.setWhere(subEqualsTo);
        subSelect.setSelectBody(subBody);

        return subSelect;
    }

}
