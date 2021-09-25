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
import java.util.stream.Stream;

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlQueryToSelect {

    private final GraphqlAntlrManager manager;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlQueryToSelect(GraphqlAntlrManager manager, GraphqlArgumentsToWhere argumentsToWhere) {
        this.manager = manager;
        this.argumentsToWhere = argumentsToWhere;
    }

//    public List<String> createSelectsSql(String graphql) {
//        return createSelectsSql(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
//    }

    public List<String> createSelectsSqlByQuery(String graphql) {
        return createSelectsSqlByQuery(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

//    public List<String> createSelectsSql(GraphqlParser.DocumentContext documentContext) {
//        return createSelects(documentContext).stream()
//                .map(Select::toString).collect(Collectors.toList());
//    }

    public List<String> createSelectsSqlByQuery(GraphqlParser.DocumentContext documentContext) {
        return createSelectsByQuery(documentContext).stream()
                .map(Select::toString).collect(Collectors.toList());
    }

//    public List<Select> createSelects(GraphqlParser.DocumentContext documentContext) {
//        return documentContext.definition().stream().flatMap(this::createSelects).collect(Collectors.toList());
//    }

    public List<Select> createSelectsByQuery(GraphqlParser.DocumentContext documentContext) {
        return documentContext.definition().stream().map(this::createSelect).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    protected Optional<Select> createSelect(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() == null) {
            return Optional.empty();
        }
        return operationDefinitionToSelect(definitionContext.operationDefinition());
    }

//    protected Stream<Select> createSelects(GraphqlParser.DefinitionContext definitionContext) {
//        if (definitionContext.operationDefinition() == null) {
//            return Stream.empty();
//        }
//        return operationDefinitionToSelects(definitionContext.operationDefinition());
//    }

    protected Optional<Select> operationDefinitionToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
            if (queryOperationTypeDefinition.isPresent()) {
                Select select = new Select();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
                selectExpressionItem.setExpression(objectFieldSelectionToJsonObjectFunction(queryOperationTypeDefinition.get().typeName().name().getText(),operationDefinitionContext.selectionSet().selection()));
                selectExpressionItem.setAlias(new Alias(DB_NAME_UTIL.nameToDBEscape("data")));
                body.setSelectItems(Collections.singletonList(selectExpressionItem));
                Table table = new Table("dual");
                body.setFromItem(table);
                select.setSelectBody(body);
                return Optional.of(select);
            }
        }
        return Optional.empty();
    }

//    protected Stream<Select> operationDefinitionToSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
//        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
//            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
//            if (queryOperationTypeDefinition.isPresent()) {
//                return operationDefinitionContext.selectionSet().selection().stream()
//                        .map(selectionContext -> selectionToSelect(queryOperationTypeDefinition.get().typeName().name().getText(), selectionContext));
//            }
//        }
//        return Stream.empty();
//    }

//    protected Select selectionToSelect(String typeName, GraphqlParser.SelectionContext selectionContext) {
//        Select select = new Select();
//        PlainSelect body = new PlainSelect();
//        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
//        selectExpressionItem.setExpression(selectionToExpression(typeName, selectionContext));
//        selectExpressionItem.setAlias(new Alias(DB_NAME_UTIL.nameToDBEscape(selectionContext.field().name().getText())));
//        body.setSelectItems(Collections.singletonList(selectExpressionItem));
//        Table table = new Table("dual");
//        body.setFromItem(table);
//        select.setSelectBody(body);
//        return select;
//    }

//    protected Expression selectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext) {
//        Optional<String> fieldTypeName = manager.getObjectFieldTypeName(typeName, selectionContext.field().name().getText());
//        if (fieldTypeName.isPresent()) {
//            if (manager.isObject(fieldTypeName.get())) {
//                return objectFieldToSubSelect(typeName, selectionContext);
//            } else if (manager.isScaLar(fieldTypeName.get()) || manager.isEnum(fieldTypeName.get())) {
//                return scalarFieldToColumn(typeName, selectionContext);
//            }
//        }
//        return null;
//    }

    protected Column scalarFieldToColumn(String typeName, GraphqlParser.SelectionContext selectionContext) {

        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(typeName);
        Table table = new Table(tableName);
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(selectionContext.field().name().getText()));
    }

    protected SubSelect objectFieldToSubSelect(String typeName, GraphqlParser.SelectionContext selectionContext) {

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());
        if (fieldDefinitionContext.isPresent()) {
            SubSelect subSelect = new SubSelect();
            PlainSelect body = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(selectionToJsonFunction(fieldDefinitionContext.get(), selectionContext));
            body.setSelectItems(Collections.singletonList(selectExpressionItem));
            subSelect.setSelectBody(body);
            Table subTable = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.get().type())));
            body.setFromItem(subTable);

            if (manager.isQueryOperationType(typeName)) {
                body.setWhere(argumentsToWhere.argumentsToMultipleExpression(fieldDefinitionContext.get().type(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments()));
            } else if (manager.isMutationOperationType(typeName)) {
                EqualsTo equalsTo = new EqualsTo();
                manager.getObjectTypeIDFieldName(manager.getFieldTypeName(fieldDefinitionContext.get().type()))
                        .map(objectTypeIDFieldName -> new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectTypeIDFieldName)))
                        .ifPresent(equalsTo::setLeftExpression);
                Optional<GraphqlParser.ArgumentContext> idArgument = manager.getIDArgument(fieldDefinitionContext.get().type(), selectionContext.field().arguments());
                if (idArgument.isPresent()) {
                    equalsTo.setRightExpression(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idArgument.get().valueWithVariable()));
                } else {
                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                    manager.getObjectTypeIDFieldName(fieldTypeName)
                            .map(objectTypeIDFieldName -> DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, objectTypeIDFieldName, 0, 0))
                            .ifPresent(equalsTo::setRightExpression);
                }
                body.setWhere(equalsTo);
            } else {
                Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(typeName));
                Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(typeName, fieldDefinitionContext.get());
                Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext.get());
                if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
                    Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext.get());
                    EqualsTo equalsTo = new EqualsTo();
                    if (mapWithTypeArgument.isPresent()) {
                        Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                        Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                        Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                        if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {

                            Table mapWithTable = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(mapWithTypeName.get()));
                            EqualsTo mapWithEqualsTo = new EqualsTo();
                            mapWithEqualsTo.setLeftExpression(new Column(mapWithTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapWithToFieldName.get())));
                            mapWithEqualsTo.setRightExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldDefinition.get().name().getText())));

                            Join join = new Join();
                            join.setLeft(true);
                            join.setRightItem(mapWithTable);
                            join.setOnExpression(mapWithEqualsTo);
                            body.setJoins(Collections.singletonList(join));

                            equalsTo.setLeftExpression(new Column(mapWithTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapWithFromFieldName.get())));
                            equalsTo.setRightExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldDefinition.get().name().getText())));
                        }
                    } else {

                        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldDefinition.get().name().getText())));
                        equalsTo.setRightExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldDefinition.get().name().getText())));
                    }
                    if (selectionContext.field().arguments() != null) {
                        Optional<GraphqlParser.FieldDefinitionContext> queryFieldDefinition = manager.getQueryOperationFieldDefinitionContext(manager.getFieldTypeName(fieldDefinitionContext.get().type()), manager.fieldTypeIsList(fieldDefinitionContext.get().type()));
                        if (queryFieldDefinition.isPresent()) {
                            body.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, argumentsToWhere.argumentsToMultipleExpression(fieldDefinitionContext.get().type(), queryFieldDefinition.get().argumentsDefinition(), selectionContext.field().arguments()))));
                        } else {
                            //TODO
                        }
                    } else {
                        body.setWhere(equalsTo);
                    }
                }
            }
            return subSelect;
        }
        return null;
    }

    protected Expression selectionToJsonFunction(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            return listFieldSelectionToJsonArrayFunction(fieldDefinitionContext, selectionContext.field().selectionSet().selection());
        } else {
            return objectFieldSelectionToJsonObjectFunction(fieldDefinitionContext, selectionContext.field().selectionSet().selection());
        }
    }

    protected Expression listFieldSelectionToJsonArrayFunction(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, List<GraphqlParser.SelectionContext> selectionContexts) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (manager.isObject(typeName)) {
            function.setParameters(new ExpressionList(objectFieldSelectionToJsonObjectFunction(fieldDefinitionContext, selectionContexts)));
        } else if (manager.isScaLar(typeName) || manager.isEnum(typeName)) {
            function.setParameters(new ExpressionList(new Column(new Table(typeName), fieldDefinitionContext.name().getText())));
        }
        return function;
    }

    protected Expression objectFieldSelectionToJsonObjectFunction(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext, List<GraphqlParser.SelectionContext> selectionContexts) {

        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(new ExpressionList(selectionContexts.stream()
                .map(selectionContext -> manager.getObjectFieldDefinitionContext(operationTypeDefinitionContext.typeName().name().getText(),
                        selectionContext.field().name().getText()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(fieldDefinitionContext -> new ExpressionList(new StringValue(fieldDefinitionContext.name().getText()),
                        selectionToJsonFunction(fieldDefinitionContext, selectionContexts)))
                .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));
        return function;
    }

    protected Expression objectFieldSelectionToJsonObjectFunction(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, List<GraphqlParser.SelectionContext> selectionContexts) {

        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (manager.isObject(typeName)) {
            Function function = new Function();
            function.setName("JSON_OBJECT");
            function.setParameters(new ExpressionList(selectionContexts.stream()
                    .map(selectionContext -> new ExpressionList(new StringValue(selectionContext.field().name().getText()), selectionToJsonFunction(fieldDefinitionContext, selectionContext)))
                    .map(ExpressionList::getExpressions).flatMap(Collection::stream).collect(Collectors.toList())));
            return function;
        } else if (manager.isScaLar(typeName) || manager.isEnum(typeName)) {
            return new Column(new Table(typeName), fieldDefinitionContext.name().getText());
        }
        return null;
    }
}
