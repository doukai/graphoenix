package io.graphoenix.mygql.parser;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.mygql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mygql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlQueryToSelect {

    private final GraphqlAntlrManager manager;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlQueryToSelect(GraphqlAntlrManager manager, GraphqlArgumentsToWhere argumentsToWhere) {
        this.manager = manager;
        this.argumentsToWhere = argumentsToWhere;
    }

    public List<Select> createSelects(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createSelect).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    protected Optional<Select> createSelect(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() == null) {
            return Optional.empty();
        }
        return operationDefinitionToSelect(definitionContext.operationDefinition());
    }

    protected Optional<Select> operationDefinitionToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().getText().equals("query")) {
            Select select = new Select();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(objectFieldSelectionToJsonObjectFunction(null, operationDefinitionContext.selectionSet().selection()));
            body.setSelectItems(Collections.singletonList(selectExpressionItem));

            Table table = new Table("dual");
            body.setFromItem(table);
            select.setSelectBody(body);

            return Optional.of(select);
        }
        return Optional.empty();
    }

    protected Expression selectionToExpression(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? manager.getQueryOperationTypeName() : manager.getFieldTypeName(typeContext);
        String filedTypeName = manager.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());
        if (manager.isObject(filedTypeName)) {
            return objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext);
        } else if (manager.isScaLar(filedTypeName)) {
            if (typeContext != null) {
                return scaLarFieldToColumn(typeContext, selectionContext);
            }
        }
        return null;
    }

    protected Column scaLarFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
    }

    protected SubSelect objectFieldToSubSelect(String typeName, String filedTypeName, GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        return objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext, false);
    }

    protected SubSelect objectFieldToSubSelect(String typeName, String filedTypeName, GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext, boolean isMutation) {

        Optional<GraphqlParser.TypeContext> fieldTypeContext = manager.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());
        if (fieldTypeContext.isPresent()) {
            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

            selectExpressionItem.setExpression(selectionToJsonFunction(fieldTypeContext.get(), selectionContext));

            body.setSelectItems(Collections.singletonList(selectExpressionItem));
            subSelect.setSelectBody(body);

            Table subTable = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldTypeContext.get())));
            body.setFromItem(subTable);

            if (typeContext != null) {
                String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext));
                Table table = new Table(tableName);
                EqualsTo equalsTo = new EqualsTo();

                if (manager.fieldTypeIsList(fieldTypeContext.get())) {
                    equalsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeRelationFieldName(filedTypeName, typeName))));
                    equalsTo.setRightExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(typeName))));
                    if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                        body.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, argumentsToWhere.argumentsToMultipleExpression(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()))));
                    } else {
                        body.setWhere(equalsTo);
                    }
                } else {
                    equalsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(filedTypeName))));
                    equalsTo.setRightExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(selectionContext.field().name().getText())));
                    body.setWhere(equalsTo);
                }
            } else {
                if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                    if (isMutation) {
                        EqualsTo equalsTo = new EqualsTo();
                        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldTypeContext.get())));
                        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(manager.getObjectTypeIDFieldName(manager.getFieldTypeName(fieldTypeContext.get())))));
                        Optional<GraphqlParser.ArgumentContext> idArgument = manager.getIDArgument(fieldTypeContext.get(), selectionContext.field().arguments());
                        if (idArgument.isPresent()) {
                            equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idArgument.get().valueWithVariable()));
                        } else {
                            String fieldTypeName = manager.getFieldTypeName(fieldTypeContext.get());
                            equalsTo.setRightExpression(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, manager.getObjectTypeIDFieldName(fieldTypeName)));
                        }
                        body.setWhere(equalsTo);
                    } else {
                        body.setWhere(argumentsToWhere.argumentsToMultipleExpression(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()));
                    }
                }
            }
            return subSelect;
        }
        return null;
    }

    protected Function selectionToJsonFunction(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        if (manager.fieldTypeIsList(typeContext)) {
            return listFieldSelectionToJsonArrayFunction(typeContext, selectionContext.field().selectionSet().selection());
        } else {
            return objectFieldSelectionToJsonObjectFunction(typeContext, selectionContext.field().selectionSet().selection());
        }
    }

    protected Function listFieldSelectionToJsonArrayFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(new ExpressionList(objectFieldSelectionToJsonObjectFunction(typeContext, selectionContexts)));

        return function;
    }

    protected Function objectFieldSelectionToJsonObjectFunction(GraphqlParser.TypeContext typeContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContexts.stream()
                .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), selectionToExpression(typeContext, selectionContext)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));

        return function;
    }
}
