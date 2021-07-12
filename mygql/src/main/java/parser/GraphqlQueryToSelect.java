package parser;

import graphql.parser.antlr.GraphqlParser;
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

public class GraphqlQueryToSelect {

    private final GraphqlAntlrRegister register;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlQueryToSelect(GraphqlAntlrRegister register, GraphqlArgumentsToWhere argumentsToWhere) {
        this.register = register;
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
        String typeName = typeContext == null ? register.getQueryTypeName() : register.getFieldTypeName(typeContext);
        String filedTypeName = register.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());
        if (register.isObject(filedTypeName)) {
            return objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext);
        } else if (register.isScaLar(filedTypeName) || register.isInnerScalar(filedTypeName)) {
            if (typeContext != null) {
                return scaLarFieldToColumn(typeContext, selectionContext);
            }
        }
        return null;
    }

    protected Column scaLarFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        return new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
    }

    protected SubSelect objectFieldToSubSelect(String typeName, String filedTypeName, GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {

        return objectFieldToSubSelect(typeName, filedTypeName, typeContext, selectionContext, false);
    }

    protected SubSelect objectFieldToSubSelect(String typeName, String filedTypeName, GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext, boolean isMutation) {

        Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());
        if (fieldTypeContext.isPresent()) {
            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();

            selectExpressionItem.setExpression(selectionToJsonFunction(fieldTypeContext.get(), selectionContext));

            body.setSelectItems(Collections.singletonList(selectExpressionItem));
            subSelect.setSelectBody(body);

            Table subTable = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext.get())));
            body.setFromItem(subTable);

            if (typeContext != null) {
                String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
                Table table = new Table(tableName);
                EqualsTo equalsTo = new EqualsTo();

                if (register.fieldTypeIsList(fieldTypeContext.get())) {
                    equalsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(filedTypeName, typeName))));
                    equalsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(typeName))));
                    if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                        body.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, argumentsToWhere.argumentsToMultipleExpression(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()))));
                    } else {
                        body.setWhere(equalsTo);
                    }
                } else {
                    equalsTo.setLeftExpression(new Column(subTable, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(filedTypeName))));
                    equalsTo.setRightExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(selectionContext.field().name().getText())));
                    body.setWhere(equalsTo);
                }
            } else {
                if (fieldDefinitionContext.isPresent() && selectionContext.field().arguments() != null) {
                    if (isMutation) {
                        EqualsTo equalsTo = new EqualsTo();
                        Table table = new Table(DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext.get())));
                        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(fieldTypeContext.get())))));
                        Optional<GraphqlParser.ArgumentContext> idArgument = register.getIdArgument(fieldTypeContext.get(), selectionContext.field().arguments());
                        if (idArgument.isPresent()) {
                            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idArgument.get().valueWithVariable()));
                        } else {
                            equalsTo.setRightExpression(register.createInsertIdUserVariable(fieldTypeContext.get()));
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
        if (register.fieldTypeIsList(typeContext)) {
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
