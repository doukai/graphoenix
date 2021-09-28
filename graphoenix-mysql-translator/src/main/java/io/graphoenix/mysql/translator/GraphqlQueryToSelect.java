package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlQueryToSelect {

    private final GraphqlAntlrManager manager;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlQueryToSelect(GraphqlAntlrManager manager, GraphqlArgumentsToWhere argumentsToWhere) {
        this.manager = manager;
        this.argumentsToWhere = argumentsToWhere;
    }

    public List<String> createSelectsSql(String graphql) {
        return createSelectsSql(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    public List<String> createSelectsSql(GraphqlParser.DocumentContext documentContext) {
        return createSelects(documentContext).stream()
                .map(Select::toString).collect(Collectors.toList());
    }

    public List<Select> createSelects(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createSelect).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public Optional<Select> createSelect(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() == null) {
            return Optional.empty();
        }
        return operationDefinitionToSelect(definitionContext.operationDefinition());
    }

    public Optional<Select> operationDefinitionToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
            if (queryOperationTypeDefinition.isPresent()) {
                return Optional.of(objectSelectionToSelect(queryOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()));
            }
        }
        return Optional.empty();
    }

    public Select objectSelectionToSelect(String typeName, List<GraphqlParser.SelectionContext> selectionContextList) {
        Select select = new Select();
        select.setSelectBody(objectSelectionToPlainSelect(null, typeName, null, selectionContextList));
        return select;
    }

    protected PlainSelect objectSelectionToPlainSelect(String parentTypeName, String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, List<GraphqlParser.SelectionContext> selectionContextList) {
        PlainSelect plainSelect = new PlainSelect();
        Table table = typeToTable(typeName);
        plainSelect.setFromItem(table);
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContextList.stream()
                .map(selectionContext -> new ExpressionList(selectionToStringValueKey(selectionContext), selectionToExpression(typeName, selectionContext)))
                .map(ExpressionList::getExpressions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())));
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

        if (manager.isQueryOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (manager.isMutationOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else {
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentTypeName, fieldDefinitionContext);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);

            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentTypeName);
                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

                if (mapWithTypeArgument.isPresent()) {
                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                        Table withTable = typeToTable(mapWithTypeName.get());

                        Join joinWithTable = new Join();
                        EqualsTo joinWithTableEqualsColumn = new EqualsTo();
                        joinWithTableEqualsColumn.setLeftExpression(fieldToColumn(mapWithTypeName.get(), mapWithToFieldName.get()));
                        joinWithTableEqualsColumn.setRightExpression(fieldToColumn(typeName, toFieldDefinition.get()));
                        joinWithTable.setLeft(true);
                        joinWithTable.setRightItem(withTable);
                        joinWithTable.setOnExpression(joinWithTableEqualsColumn);

                        Join joinParentTable = new Join();
                        EqualsTo joinTableEqualsParentColumn = new EqualsTo();
                        joinTableEqualsParentColumn.setLeftExpression(fieldToColumn(mapWithTypeName.get(), mapWithFromFieldName.get()));
                        joinTableEqualsParentColumn.setRightExpression(fieldToColumn(parentTypeName, fromFieldDefinition.get()));
                        joinParentTable.setLeft(true);
                        joinParentTable.setRightItem(parentTable);
                        joinParentTable.setOnExpression(joinTableEqualsParentColumn);

                        plainSelect.setJoins(Arrays.asList(joinWithTable, joinParentTable));
                    }
                } else {
                    EqualsTo equalsParentColumn = new EqualsTo();
                    equalsParentColumn.setLeftExpression(fieldToColumn(typeName, toFieldDefinition.get()));
                    equalsParentColumn.setRightExpression(fieldToColumn(parentTypeName, fromFieldDefinition.get()));
                    plainSelect.setWhere(equalsParentColumn);
                }
            }
        }
        return plainSelect;
    }

    protected Expression selectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext) {
        return manager.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText())
                .map(fieldDefinitionContext -> {
                            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                return listSelectionToJsonArrayFunction(typeName, selectionContext);
                            } else {
                                return singleSelectionToExpression(typeName, selectionContext);
                            }
                        }
                ).orElse(null);
    }

    protected Function listSelectionToJsonArrayFunction(String typeName, GraphqlParser.SelectionContext selectionContext) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(new ExpressionList(singleSelectionToExpression(typeName, selectionContext)));
        return function;
    }

    protected Expression singleSelectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext) {
        return manager.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText())
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext);
                            } else {
                                return fieldToExpression(typeName, fieldDefinitionContext);
                            }
                        }
                ).orElse(null);
    }

    protected SubSelect objectSelectionToSubSelect(String parentTypeName,
                                                   String typeName,
                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                   GraphqlParser.SelectionContext selectionContext) {
        SubSelect subSelect = new SubSelect();
        PlainSelect plainSelect = objectSelectionToPlainSelect(parentTypeName, typeName, fieldDefinitionContext, selectionContext.field().selectionSet().selection());

        if (manager.isMutationOperationType(parentTypeName)) {
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);

            if (idFieldName.isPresent()) {
                Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), selectionContext.field().arguments())
                        .map(DB_VALUE_UTIL::createIdValueExpression)
                        .orElse(DB_VALUE_UTIL.createInsertIdUserVariable(typeName, idFieldName.get(), 0, 0));

                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(fieldToColumn(typeName, idFieldName.get()));
                idEqualsTo.setRightExpression(idValueExpression);
                plainSelect.setWhere(idEqualsTo);
            }
        } else {
            if (selectionContext.field().arguments() != null) {
                Optional<Expression> where = argumentsToWhere.argumentsToMultipleExpression(fieldDefinitionContext, selectionContext.field().arguments());
                where.ifPresent(expression -> {
                    if (plainSelect.getWhere() != null) {
                        plainSelect.setWhere(new MultiAndExpression(Arrays.asList(plainSelect.getWhere(), expression)));
                    } else {
                        plainSelect.setWhere(expression);
                    }
                });
            }
        }
        subSelect.setSelectBody(plainSelect);
        return subSelect;
    }

    protected Expression fieldToExpression(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(typeName, fieldDefinitionContext);
            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

            if (fromFieldDefinition.isPresent() && mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    SubSelect subSelect = new SubSelect();
                    PlainSelect plainSelect = new PlainSelect();
                    Table table = typeToTable(mapWithTypeName.get());
                    plainSelect.setFromItem(table);
                    plainSelect.setSelectItems(Collections.singletonList(new SelectExpressionItem(fieldToColumn(mapWithTypeName.get(), mapWithToFieldName.get()))));
                    EqualsTo equalsTo = new EqualsTo();
                    equalsTo.setLeftExpression(fieldToColumn(mapWithTypeName.get(), mapWithFromFieldName.get()));
                    equalsTo.setRightExpression(fieldToColumn(typeName, fromFieldDefinition.get()));
                    plainSelect.setWhere(equalsTo);
                    subSelect.setSelectBody(plainSelect);
                    return subSelect;
                }
            }
        } else {
            return fieldToColumn(typeName, fieldDefinitionContext);
        }
        return null;
    }

    protected StringValue selectionToStringValueKey(GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.field().alias() != null) {
            return new StringValue(selectionContext.field().alias().getText());
        } else {
            return new StringValue(selectionContext.field().name().getText());
        }
    }

    protected Table typeToTable(String typeName) {
        if (manager.isQueryOperationType(typeName)) {
            return new Table("dual");
        }
        return new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(typeName));
    }

    protected Column fieldToColumn(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Column(typeToTable(typeName), DB_NAME_UTIL.graphqlTypeNameToTableName(fieldDefinitionContext.name().getText()));
    }

    protected Column fieldToColumn(String typeName, String fieldName) {
        return new Column(typeToTable(typeName), DB_NAME_UTIL.graphqlTypeNameToTableName(fieldName));
    }
}
