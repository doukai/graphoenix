package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.mysql.utils.DBNameUtil;
import io.graphoenix.mysql.utils.DBValueUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OBJECT_SELECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OPERATION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AFTER_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.BEFORE_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.OFFSET_INPUT_NAME;

@ApplicationScoped
public class GraphQLQueryToSelect {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;
    private final GraphQLArgumentsToWhere argumentsToWhere;
    private final DBNameUtil dbNameUtil;
    private final DBValueUtil dbValueUtil;

    @Inject
    public GraphQLQueryToSelect(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, GraphQLArgumentsToWhere argumentsToWhere, DBNameUtil dbNameUtil, DBValueUtil dbValueUtil) {
        this.manager = manager;
        this.mapper = mapper;
        this.argumentsToWhere = argumentsToWhere;
        this.dbNameUtil = dbNameUtil;
        this.dbValueUtil = dbValueUtil;
    }

    public String createSelectSQL(String graphQL) {
        return operationDefinitionToSelect(DOCUMENT_UTIL.graphqlToOperation(graphQL)).toString();
    }

    public Stream<Tuple2<String, String>> createSelectsSQL(String graphQL) {
        return operationDefinitionToSelects(DOCUMENT_UTIL.graphqlToOperation(graphQL)).map(result -> Tuple.of(result._1(), result._2().toString()));
    }

    public Select createSelect(String graphQL) {
        return operationDefinitionToSelect(DOCUMENT_UTIL.graphqlToOperation(graphQL));
    }

    public Stream<Tuple2<String, Select>> createSelects(String graphQL) {
        return operationDefinitionToSelects(DOCUMENT_UTIL.graphqlToOperation(graphQL));
    }

    public String createSelectSQL(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToSelect(operationDefinitionContext).toString();
    }

    public Stream<Tuple2<String, String>> createSelectsSQL(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToSelects(operationDefinitionContext).map(result -> Tuple.of(result._1(), result._2().toString()));
    }

    public Select createSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToSelect(operationDefinitionContext);
    }

    public Stream<Tuple2<String, Select>> createSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToSelects(operationDefinitionContext);
    }

    public Select operationDefinitionToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
            if (queryOperationTypeDefinition.isPresent()) {
                return objectSelectionToSelect(queryOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection());
            } else {
                throw new GraphQLProblem().push(OPERATION_NOT_EXIST);
            }
        }
        throw new GraphQLProblem().push(QUERY_NOT_EXIST);
    }

    public Stream<Tuple2<String, Select>> operationDefinitionToSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
            if (queryOperationTypeDefinition.isPresent()) {
                if (operationDefinitionContext.selectionSet() == null || operationDefinitionContext.selectionSet().selection().size() == 0) {
                    throw new GraphQLProblem(SELECTION_NOT_EXIST.bind(queryOperationTypeDefinition.get().getText()));
                }
                return operationDefinitionContext.selectionSet().selection().stream()
                        .map(selectionContext ->
                                Tuple.of(
                                        selectionContext.field().name().getText(),
                                        manager.getQueryOperationTypeDefinition().map(
                                                operationTypeDefinitionContext ->
                                                        objectSelectionToSelect(operationTypeDefinitionContext.typeName().name().getText(), selectionContext.field().selectionSet().selection())
                                        )
                                )
                        )
                        .filter(result -> result._2().isPresent())
                        .map(result -> Tuple.of(result._1(), result._2().get()));
            }
        }
        throw new GraphQLProblem().push(QUERY_NOT_EXIST);
    }

    public Select objectSelectionToSelect(String typeName, List<GraphqlParser.SelectionContext> selectionContextList) {
        Select select = new Select();
        select.setSelectBody(objectSelectionToPlainSelect(typeName, selectionContextList));
        return select;
    }

    protected PlainSelect objectSelectionToPlainSelect(String typeName, List<GraphqlParser.SelectionContext> selectionContextList) {
        return objectSelectionToPlainSelect(null, typeName, null, selectionContextList, 0);
    }

    protected PlainSelect objectSelectionToPlainSelect(String parentTypeName, String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, List<GraphqlParser.SelectionContext> selectionContextList, int level) {
        PlainSelect plainSelect = new PlainSelect();
        Table table = typeToTable(typeName, level);
        plainSelect.setFromItem(table);

        Function function = selectionToJsonFunction(
                fieldDefinitionContext,
                new ExpressionList(
                        selectionContextList.stream()
                                .flatMap(selectionContext -> manager.fragmentUnzip(typeName, selectionContext))
                                .filter(selectionContext -> manager.isNotInvokeField(typeName, selectionContext.field().name().getText()))
                                .map(selectionContext ->
                                        new ExpressionList(
                                                selectionToStringValueKey(selectionContext), selectionToExpression(typeName, selectionContext, level)
                                        )
                                )
                                .map(ExpressionList::getExpressions)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                )
        );

        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
        selectExpressionItem.setAlias(new Alias("json_object"));
        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

        if (manager.isQueryOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (manager.isMutationOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else {
            if (!manager.isQueryOperationType(parentTypeName) && !manager.isMutationOperationType(parentTypeName) && !manager.isSubscriptionOperationType(parentTypeName)) {
                String fieldName = fieldDefinitionContext.name().getText();
                Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentTypeName, fieldName);

                if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
                    Table parentTable = typeToTable(parentTypeName, level - 1);
                    boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);
                    EqualsTo equalsParentColumn = new EqualsTo();
                    equalsParentColumn.setLeftExpression(fieldToColumn(table, toFieldDefinition.get()));

                    if (mapWithType) {
                        Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                        if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                            Table withTable = typeToTable(mapWithObjectDefinition.get().name().getText(), level);
                            SubSelect selectWithTable = new SubSelect();
                            PlainSelect subPlainSelect = new PlainSelect();
                            subPlainSelect.setSelectItems(Collections.singletonList(new SelectExpressionItem(fieldToColumn(withTable, mapWithToFieldDefinition.get()))));
                            EqualsTo equalsWithTableColumn = new EqualsTo();
                            equalsWithTableColumn.setLeftExpression(fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                            equalsWithTableColumn.setRightExpression(fieldToColumn(parentTable, fromFieldDefinition.get()));
                            subPlainSelect.setWhere(equalsWithTableColumn);
                            subPlainSelect.setFromItem(withTable);
                            selectWithTable.setSelectBody(subPlainSelect);
                            equalsParentColumn.setRightExpression(selectWithTable);
                        } else {
                            GraphQLProblem graphQLProblem = new GraphQLProblem();
                            if (mapWithObjectDefinition.isEmpty()) {
                                graphQLProblem.push(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                            }
                            if (mapWithFromFieldDefinition.isEmpty()) {
                                graphQLProblem.push(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                            }
                            if (mapWithToFieldDefinition.isEmpty()) {
                                graphQLProblem.push(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                            }
                            throw graphQLProblem;
                        }
                    } else {
                        equalsParentColumn.setRightExpression(fieldToColumn(parentTable, fromFieldDefinition.get()));
                    }
                    plainSelect.setWhere(equalsParentColumn);
                } else {
                    GraphQLProblem graphQLProblem = new GraphQLProblem();
                    if (fromFieldDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (toFieldDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLProblem;
                }
            }
            if (!manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                Limit limit = new Limit();
                limit.setOffset(new LongValue(0));
                limit.setRowCount(new LongValue(1));
                plainSelect.setLimit(limit);
            }
        }
        return plainSelect;
    }

    protected Expression selectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext, int level) {
        return manager.getObjectFieldDefinition(typeName, selectionContext.field().name().getText())
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                    SubSelect subSelect = new SubSelect();
                                    PlainSelect plainSelect = new PlainSelect();
                                    plainSelect.addSelectItems(new SelectExpressionItem(jsonArrayFunction(new ExpressionList(jsonExtractFunction(new Column("json_object"))))));
                                    SubSelect objectArrayTable = objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext, level + 1);
                                    String aliasName = typeName.concat("_").concat(fieldTypeName).concat("_").concat(String.valueOf(level + 1));
                                    objectArrayTable.setAlias(new Alias(aliasName));
                                    plainSelect.setFromItem(objectArrayTable);
                                    subSelect.setSelectBody(plainSelect);
                                    return jsonExtractFunction(subSelect);
                                } else {
                                    return jsonExtractFunction(objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext, level + 1));
                                }
                            } else {
                                return fieldToExpression(typeName, fieldDefinitionContext, selectionContext, level);
                            }
                        }
                )
                .orElseThrow(() -> {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText()));
                });
    }

    protected Function selectionToJsonFunction(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, ExpressionList expressionList) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(expressionList);
//        if (fieldDefinitionContext != null && manager.fieldTypeIsList(fieldDefinitionContext.type())) {
//            return jsonArrayFunction(new ExpressionList(function));
//        }
        return function;
    }

    protected Function jsonArrayFunction(ExpressionList expressionList) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(expressionList);
        return function;
    }

    protected Function jsonExtractFunction(Expression expression) {
        Function function = new Function();
        function.setName("JSON_EXTRACT");
        function.setParameters(new ExpressionList(Arrays.asList(expression, new StringValue("$"))));
        return function;
    }

    protected SubSelect objectSelectionToSubSelect(String parentTypeName,
                                                   String typeName,
                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                   GraphqlParser.SelectionContext selectionContext,
                                                   int level) {
        SubSelect subSelect = new SubSelect();
        if (selectionContext.field().selectionSet() == null || selectionContext.field().selectionSet().selection().size() == 0) {
            throw new GraphQLProblem(OBJECT_SELECTION_NOT_EXIST.bind(fieldDefinitionContext.getText()));
        }
        PlainSelect plainSelect = objectSelectionToPlainSelect(parentTypeName, typeName, fieldDefinitionContext, selectionContext.field().selectionSet().selection(), level);

        if (manager.isMutationOperationType(parentTypeName)) {
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);

            if (idFieldName.isPresent()) {
                Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), selectionContext.field().arguments())
                        .flatMap(dbValueUtil::createIdValueExpression)
                        .orElseGet(() -> dbValueUtil.createInsertIdUserVariable(typeName, idFieldName.get(), 0, 0));

                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(fieldToColumn(typeToTable(typeName, level), idFieldName.get()));
                idEqualsTo.setRightExpression(idValueExpression);
                plainSelect.setWhere(idEqualsTo);
            } else {
                throw new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
            }
        } else {
            Optional<Expression> where = argumentsToWhere.argumentsToMultipleExpression(fieldDefinitionContext, selectionContext.field().arguments(), level);
            where.ifPresent(expression -> {
                if (plainSelect.getWhere() != null) {
                    plainSelect.setWhere(new MultiAndExpression(Arrays.asList(plainSelect.getWhere(), expression)));
                } else {
                    plainSelect.setWhere(expression);
                }
            });
        }
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(typeName, "cursor").findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                    .orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));
            Column cursorColumn = dbNameUtil.fieldToColumn(typeName, cursorFieldDefinitionContext, level);
            buildPageArguments(plainSelect, fieldDefinitionContext, selectionContext, cursorColumn);
        }
        subSelect.setSelectBody(plainSelect);
        return subSelect;
    }

    protected void buildPageArguments(PlainSelect plainSelect, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, Column cursorColumn) {

        if (fieldDefinitionContext.argumentsDefinition() != null) {
            Optional<GraphqlParser.InputValueDefinitionContext> firstInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(FIRST_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> offsetInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(OFFSET_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> afterInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(AFTER_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> beforeInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(BEFORE_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.ArgumentContext> first = firstInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            Optional<GraphqlParser.ArgumentContext> offset = offsetInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            Optional<GraphqlParser.ArgumentContext> after = afterInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            Optional<GraphqlParser.ArgumentContext> before = beforeInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));

            if (first.isPresent()) {
                Limit limit = new Limit();
                limit.setRowCount(dbValueUtil.valueWithVariableToDBValue(first.get().valueWithVariable()));
                offset.ifPresent(argumentContext -> limit.setOffset(dbValueUtil.valueWithVariableToDBValue(argumentContext.valueWithVariable())));
                plainSelect.setLimit(limit);
            } else {
                if (firstInput.isPresent() && firstInput.get().defaultValue() != null) {
                    Limit limit = new Limit();
                    limit.setRowCount(dbValueUtil.valueToDBValue(firstInput.get().defaultValue().value()));
                    offset.ifPresent(argumentContext -> limit.setOffset(dbValueUtil.valueWithVariableToDBValue(argumentContext.valueWithVariable())));
                    plainSelect.setLimit(limit);
                }
            }
            if (after.isPresent()) {
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(cursorColumn);
                greaterThan.setRightExpression(dbValueUtil.scalarValueWithVariableToDBValue(after.get().valueWithVariable()));
                if (plainSelect.getWhere() != null) {
                    plainSelect.setWhere(new MultiAndExpression(Arrays.asList(plainSelect.getWhere(), greaterThan)));
                } else {
                    plainSelect.setWhere(greaterThan);
                }
            } else if (before.isPresent()) {
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(cursorColumn);
                minorThan.setRightExpression(dbValueUtil.scalarValueWithVariableToDBValue(before.get().valueWithVariable()));
                if (plainSelect.getWhere() != null) {
                    plainSelect.setWhere(new MultiAndExpression(Arrays.asList(plainSelect.getWhere(), minorThan)));
                } else {
                    plainSelect.setWhere(minorThan);
                }
            }
        }
    }

    protected Expression fieldToExpression(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, int level) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            String fieldName = fieldDefinitionContext.name().getText();
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(typeName, fieldName);
            boolean mapWithType = mapper.mapWithType(typeName, fieldName);

            if (fromFieldDefinition.isPresent() && mapWithType) {
                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(typeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(typeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(typeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    String mapWithObjectName = mapWithObjectDefinition.get().name().getText();
                    SubSelect arraySubSelect = new SubSelect();
                    PlainSelect arrayPlainSelect = new PlainSelect();
                    arrayPlainSelect.addSelectItems(new SelectExpressionItem(jsonArrayFunction(new ExpressionList(new Column(fieldDefinitionContext.name().getText())))));

                    SubSelect subSelect = new SubSelect();
                    PlainSelect plainSelect = new PlainSelect();
                    Table witTable = typeToTable(mapWithObjectName, level);
                    plainSelect.setFromItem(witTable);
                    Expression selectItem;
                    if (fieldTypeName.equals("Boolean")) {
                        Function function = new Function();
                        function.setName("IF");
                        function.setParameters(new ExpressionList(Arrays.asList(fieldToColumn(witTable, mapWithToFieldDefinition.get()), new HexValue("TRUE"), new HexValue("FALSE"))));
                        selectItem = function;
                    } else {
                        selectItem = fieldToColumn(witTable, mapWithToFieldDefinition.get());
                    }

                    SelectExpressionItem selectExpressionItem = new SelectExpressionItem(selectItem);
                    selectExpressionItem.setAlias(new Alias(fieldDefinitionContext.name().getText()));
                    plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));
                    EqualsTo equalsTo = new EqualsTo();
                    equalsTo.setLeftExpression(fieldToColumn(witTable, mapWithFromFieldDefinition.get()));
                    equalsTo.setRightExpression(fieldToColumn(typeToTable(typeName, level), fromFieldDefinition.get()));
                    plainSelect.setWhere(equalsTo);
                    subSelect.setSelectBody(plainSelect);
                    String aliasName = typeName.concat("_").concat(fieldTypeName).concat("_").concat(String.valueOf(level + 1));
                    subSelect.setAlias(new Alias(aliasName));

                    argumentsToWhere.operatorArgumentsToExpression(fieldToColumn(witTable, mapWithToFieldDefinition.get()), fieldDefinitionContext, selectionContext.field().arguments())
                            .ifPresent(plainSelect::setWhere);

                    GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(mapWithObjectName, "cursor").findFirst()
                            .or(() -> manager.getObjectTypeIDFieldDefinition(mapWithObjectName))
                            .orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(mapWithObjectName)));
                    Column cursorColumn = dbNameUtil.fieldToColumn(mapWithObjectName, cursorFieldDefinitionContext, level);
                    buildPageArguments(plainSelect, fieldDefinitionContext, selectionContext, cursorColumn);

                    arrayPlainSelect.setFromItem(subSelect);
                    arraySubSelect.setSelectBody(arrayPlainSelect);
                    return jsonExtractFunction(arraySubSelect);
                } else {
                    GraphQLProblem graphQLProblem = new GraphQLProblem();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithToFieldDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLProblem;
                }
            } else {
                GraphQLProblem graphQLProblem = new GraphQLProblem();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLProblem;
            }
        } else {
            Table table = typeToTable(typeName, level);
            if (fieldTypeName.equals("Boolean")) {
                Function function = new Function();
                function.setName("IF");
                function.setParameters(new ExpressionList(Arrays.asList(fieldToColumn(table, fieldDefinitionContext), new HexValue("TRUE"), new HexValue("FALSE"))));
                return function;
            } else {
                return fieldToColumn(table, fieldDefinitionContext);
            }
        }
    }

    protected StringValue selectionToStringValueKey(GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.field().alias() != null) {
            return new StringValue(selectionContext.field().alias().getText());
        } else {
            return new StringValue(selectionContext.field().name().getText());
        }
    }

    protected Table typeToTable(String typeName, int level) {
        if (manager.isQueryOperationType(typeName) || manager.isMutationOperationType(typeName)) {
            return dbNameUtil.dualTable();
        }
        return dbNameUtil.typeToTable(typeName, level);
    }

    protected Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return dbNameUtil.fieldToColumn(table, fieldDefinitionContext);
    }

    protected Column fieldToColumn(Table table, String fieldName) {
        return dbNameUtil.fieldToColumn(table, fieldName);
    }
}
