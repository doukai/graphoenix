package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.mysql.expression.JsonArrayAggregateFunction;
import io.graphoenix.mysql.utils.DBNameUtil;
import io.graphoenix.mysql.utils.DBValueUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;

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
        return objectSelectionToPlainSelect(null, typeName, null, null, selectionContextList, 0);
    }

    protected PlainSelect objectSelectionToPlainSelect(String parentTypeName, String typeName, GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, List<GraphqlParser.SelectionContext> selectionContextList, int level) {
        PlainSelect plainSelect = new PlainSelect();
        Table table = typeToTable(typeName, level);

        Function function = jsonObjectFunction(
                new ExpressionList(
                        selectionContextList.stream()
                                .flatMap(subSelectionContext -> manager.fragmentUnzip(typeName, subSelectionContext))
                                .filter(subSelectionContext -> manager.isNotInvokeField(typeName, subSelectionContext.field().name().getText()))
                                .map(subSelectionContext ->
                                        new ExpressionList(
                                                selectionToStringValueKey(subSelectionContext), selectionToExpression(typeName, subSelectionContext, level)
                                        )
                                )
                                .map(ExpressionList::getExpressions)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                )
        );
        if (fieldDefinitionContext != null && manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            function = jsonArrayAggFunction(new ExpressionList(function), table, selectionContext, fieldDefinitionContext);
        }
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

        if (manager.isQueryOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (manager.isMutationOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else {
            if (fieldDefinitionContext != null && !manager.isQueryOperationType(parentTypeName) && !manager.isMutationOperationType(parentTypeName) && !manager.isSubscriptionOperationType(parentTypeName)) {
                String fieldName = fieldDefinitionContext.name().getText();
                Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentTypeName, fieldName);

                if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
                    Table parentTable = typeToTable(parentTypeName, level - 1);
                    boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);
                    EqualsTo equalsParentColumn = new EqualsTo();

                    if (mapWithType) {
                        Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                        if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                            Table withTable = typeToTable(mapWithObjectDefinition.get().name().getText(), level);
                            plainSelect.setFromItem(withTable);

                            Join join = new Join();
                            join.setLeft(true);
                            join.setRightItem(table);
                            EqualsTo joinEqualsTo = new EqualsTo();
                            joinEqualsTo.setLeftExpression(fieldToColumn(table, toFieldDefinition.get()));
                            joinEqualsTo.setRightExpression(fieldToColumn(withTable, mapWithToFieldDefinition.get()));
                            join.addOnExpression(joinEqualsTo);
                            plainSelect.addJoins(join);
                            equalsParentColumn.setLeftExpression(fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
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
                        plainSelect.setFromItem(table);
                        equalsParentColumn.setLeftExpression(fieldToColumn(table, toFieldDefinition.get()));
                    }
                    equalsParentColumn.setRightExpression(fieldToColumn(parentTable, fromFieldDefinition.get()));
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
            } else {
                plainSelect.setFromItem(table);
            }
        }
        return plainSelect;
    }

    protected Expression selectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext, int level) {
        return manager.getObjectFieldDefinition(typeName, selectionContext.field().name().getText())
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return jsonExtractFunction(objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext, level + 1), manager.fieldTypeIsList(fieldDefinitionContext.type()));
                            } else {
                                return fieldToExpression(typeName, fieldDefinitionContext, selectionContext, level);
                            }
                        }
                )
                .orElseThrow(() -> {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText()));
                });
    }

    protected Function jsonObjectFunction(ExpressionList expressionList) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(expressionList);
        return function;
    }

    protected Function jsonArrayAggFunction(ExpressionList expressionList, Table table, GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        JsonArrayAggregateFunction jsonArrayAggregateFunction = new JsonArrayAggregateFunction();
        jsonArrayAggregateFunction.setName("JSON_ARRAYAGG");
        jsonArrayAggregateFunction.setParameters(expressionList);
        buildSortArguments(jsonArrayAggregateFunction, table, fieldDefinitionContext, selectionContext);
        buildPageArguments(jsonArrayAggregateFunction, fieldDefinitionContext, selectionContext);
        return jsonArrayAggregateFunction;
    }

    protected Function jsonExtractFunction(Expression expression, boolean isList) {
        Function function = new Function();
        function.setName("JSON_EXTRACT");
        if (isList) {
            Function ifNull = new Function();
            ifNull.setName("IFNULL");

            Function jsonArray = new Function();
            jsonArray.setName("JSON_ARRAY");

            ifNull.setParameters(new ExpressionList(expression, jsonArray));
            function.setParameters(new ExpressionList(ifNull, new StringValue("$")));
        } else {
            function.setParameters(new ExpressionList(expression, new StringValue("$")));
        }
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
        PlainSelect plainSelect = objectSelectionToPlainSelect(parentTypeName, typeName, selectionContext, fieldDefinitionContext, selectionContext.field().selectionSet().selection(), level);
        buildCursorArguments(plainSelect, typeName, selectionContext, fieldDefinitionContext, level);

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
        buildGroupArguments(plainSelect, fieldDefinitionContext, selectionContext, level);
        subSelect.setSelectBody(plainSelect);
        if (!manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            Limit limit = new Limit();
            limit.setOffset(new LongValue(0));
            limit.setRowCount(new LongValue(1));
            plainSelect.setLimit(limit);
        }
        return subSelect;
    }

    protected void buildPageArguments(JsonArrayAggregateFunction jsonArrayAggregateFunction, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            Optional<GraphqlParser.InputValueDefinitionContext> firstInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(FIRST_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> offsetInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(OFFSET_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.ArgumentContext> first = firstInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            Optional<GraphqlParser.ArgumentContext> offset = offsetInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));

            if (first.isPresent()) {
                Limit limit = new Limit();
                limit.setRowCount(dbValueUtil.valueWithVariableToDBValue(first.get().valueWithVariable()));
                offset.ifPresent(argumentContext -> limit.setOffset(dbValueUtil.valueWithVariableToDBValue(argumentContext.valueWithVariable())));
                jsonArrayAggregateFunction.setLimit(limit);
            } else {
                if (firstInput.isPresent() && firstInput.get().defaultValue() != null) {
                    Limit limit = new Limit();
                    limit.setRowCount(dbValueUtil.valueToDBValue(firstInput.get().defaultValue().value()));
                    offset.ifPresent(argumentContext -> limit.setOffset(dbValueUtil.valueWithVariableToDBValue(argumentContext.valueWithVariable())));
                    jsonArrayAggregateFunction.setLimit(limit);
                }
            }
        }
    }

    protected void buildCursorArguments(PlainSelect plainSelect, String typeName, GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level) {
        Optional<GraphqlParser.InputValueDefinitionContext> afterInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(AFTER_INPUT_NAME))
                .findFirst();

        Optional<GraphqlParser.InputValueDefinitionContext> beforeInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(BEFORE_INPUT_NAME))
                .findFirst();

        Optional<GraphqlParser.ArgumentContext> after = afterInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
        Optional<GraphqlParser.ArgumentContext> before = beforeInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));

        GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(typeName, "cursor").findFirst()
                .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                .orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

        Column cursorColumn = dbNameUtil.fieldToColumn(typeName, cursorFieldDefinitionContext, level);

        if (after.isPresent()) {
            GreaterThan greaterThan = new GreaterThan();
            greaterThan.setLeftExpression(cursorColumn);
            greaterThan.setRightExpression(dbValueUtil.scalarValueWithVariableToDBValue(after.get().valueWithVariable()));
            plainSelect.setWhere(greaterThan);
        } else if (before.isPresent()) {
            MinorThan minorThan = new MinorThan();
            minorThan.setLeftExpression(cursorColumn);
            minorThan.setRightExpression(dbValueUtil.scalarValueWithVariableToDBValue(before.get().valueWithVariable()));
            plainSelect.setWhere(minorThan);
        }
    }

    protected void buildSortArguments(JsonArrayAggregateFunction jsonArrayAggregateFunction, Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                Optional<GraphqlParser.InputValueDefinitionContext> orderByInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(ORDER_BY_INPUT_NAME))
                        .findFirst();

                Optional<GraphqlParser.ArgumentContext> orderBy = orderByInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
                orderBy.ifPresent(argumentContext ->
                        jsonArrayAggregateFunction.setOrderByElements(
                                argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue() != null)
                                        .map(objectFieldWithVariableContext -> {
                                                    GraphqlParser.EnumValueContext enumValueContext = objectFieldWithVariableContext.valueWithVariable().enumValue();
                                                    OrderByElement orderByElement = new OrderByElement();
                                                    if (enumValueContext.enumValueName().getText().equals("DESC")) {
                                                        orderByElement.setAsc(false);
                                                    }
                                                    orderByElement.setExpression(fieldToColumn(table, objectFieldWithVariableContext));
                                                    return orderByElement;
                                                }
                                        )
                                        .collect(Collectors.toList())
                        )
                );
            } else {
                Optional<GraphqlParser.InputValueDefinitionContext> sortInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(SORT_INPUT_NAME))
                        .findFirst();
                Optional<GraphqlParser.ArgumentContext> sort = sortInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));

                if (sort.isPresent()) {
                    GraphqlParser.EnumValueContext enumValueContext = sort.get().valueWithVariable().enumValue();
                    if (enumValueContext != null) {
                        OrderByElement orderByElement = new OrderByElement();
                        if (enumValueContext.enumValueName().getText().equals("DESC")) {
                            orderByElement.setAsc(false);
                        }
                        orderByElement.setExpression(fieldToColumn(table, fieldDefinitionContext));
                        jsonArrayAggregateFunction.setOrderByElements(Collections.singletonList(orderByElement));
                    }
                }
            }
        }
    }

    protected void buildGroupArguments(PlainSelect plainSelect, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, int level) {
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            Optional<GraphqlParser.InputValueDefinitionContext> groupByInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(GROUP_BY_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.ArgumentContext> groupBy = groupByInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            groupBy.ifPresent(argumentContext -> {
                        if (argumentContext.valueWithVariable().arrayValueWithVariable() != null) {
                            GroupByElement groupByElement = new GroupByElement();
                            groupByElement.setGroupByExpressionList(
                                    new ExpressionList(
                                            argumentContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                                    .filter(valueWithVariableContext -> valueWithVariableContext.StringValue() != null)
                                                    .map(valueWithVariableContext -> fieldToColumn(typeToTable(typeName, level), DOCUMENT_UTIL.getStringValue(valueWithVariableContext.StringValue())))
                                                    .collect(Collectors.toList())
                                    )
                            );
                            plainSelect.setGroupByElement(groupByElement.withUsingBrackets(false));
                        }
                    }
            );
        }
    }

    protected Expression fieldToExpression(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, int level) {
        Optional<GraphqlParser.FieldDefinitionContext> functionFieldDefinitionContext = Optional.empty();
        if (fieldDefinitionContext.directives() != null) {
            functionFieldDefinitionContext = fieldDefinitionContext.directives().directive().stream()
                    .filter(directiveContext -> directiveContext.name().getText().equals("func"))
                    .flatMap(directiveContext -> directiveContext.arguments().argument().stream()
                            .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                            .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                            .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue())))
                    .findFirst()
                    .map(fieldName -> manager.getField(typeName, fieldName).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(typeName, fieldName))));
        }

        if (manager.fieldTypeIsList(fieldDefinitionContext.type()) ||
                functionFieldDefinitionContext.isPresent() && manager.fieldTypeIsList(functionFieldDefinitionContext.get().type())
        ) {
            String fieldName;
            if (functionFieldDefinitionContext.isPresent()) {
                fieldName = functionFieldDefinitionContext.get().name().getText();
            } else {
                fieldName = fieldDefinitionContext.name().getText();
            }
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(typeName, fieldName);
            boolean mapWithType = mapper.mapWithType(typeName, fieldName);

            if (fromFieldDefinition.isPresent() && mapWithType) {
                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(typeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(typeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(typeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    SubSelect subSelect = new SubSelect();
                    PlainSelect plainSelect = new PlainSelect();
                    Table withTable = typeToTable(mapWithObjectDefinition.get().name().getText(), level);
                    plainSelect.setFromItem(withTable);
                    Function function;

                    Optional<GraphqlParser.DirectiveContext> func = fieldDefinitionContext.directives().directive().stream()
                            .filter(directiveContext -> directiveContext.name().getText().equals("func"))
                            .findFirst();

                    if (func.isPresent()) {
                        Optional<GraphqlParser.EnumValueContext> name = func.get().arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("name"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().enumValue() != null)
                                .findFirst()
                                .map(argumentContext -> argumentContext.valueWithVariable().enumValue());

                        Optional<TerminalNode> field = func.get().arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                                .findFirst()
                                .map(argumentContext -> argumentContext.valueWithVariable().StringValue());

                        if (name.isPresent() && field.isPresent()) {
                            function = new Function();
                            function.setName(functionEnumValueToFunctionName(name.get()));
                            function.setParameters(new ExpressionList(scalarFieldToExpression(withTable, mapWithToFieldDefinition.get())));
                        } else {
                            if (name.isEmpty()) {
                                throw new GraphQLProblem(FUNC_NAME_NOT_EXIST.bind(func.get().getText()));
                            } else {
                                throw new GraphQLProblem(FUNC_FIELD_NOT_EXIST.bind(func.get().getText()));
                            }
                        }
                    } else {
                        function = jsonArrayAggFunction(
                                new ExpressionList(scalarFieldToExpression(withTable, mapWithToFieldDefinition.get())),
                                withTable,
                                selectionContext,
                                mapWithToFieldDefinition.get()
                        );
                    }
                    plainSelect.setFromItem(withTable);
                    SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
                    plainSelect.addSelectItems(selectExpressionItem);
                    buildCursorArguments(plainSelect, typeName, selectionContext, fieldDefinitionContext, level);

                    argumentsToWhere.operatorArgumentsToExpression(fieldToColumn(withTable, mapWithToFieldDefinition.get()), fieldDefinitionContext, selectionContext.field().arguments())
                            .ifPresent(expression -> {
                                if (plainSelect.getWhere() != null) {
                                    plainSelect.setWhere(new MultiAndExpression(Arrays.asList(plainSelect.getWhere(), expression)));
                                } else {
                                    plainSelect.setWhere(expression);
                                }
                            });

                    plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));
                    EqualsTo equalsTo = new EqualsTo();
                    equalsTo.setLeftExpression(fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                    equalsTo.setRightExpression(fieldToColumn(typeToTable(typeName, level), fromFieldDefinition.get()));
                    plainSelect.setWhere(equalsTo);
                    subSelect.setSelectBody(plainSelect);
                    return jsonExtractFunction(subSelect, true);
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
            if (functionFieldDefinitionContext.isPresent()) {
                Optional<GraphqlParser.DirectiveContext> func = fieldDefinitionContext.directives().directive().stream()
                        .filter(directiveContext -> directiveContext.name().getText().equals("func"))
                        .findFirst();

                if (func.isPresent()) {
                    Optional<GraphqlParser.EnumValueContext> name = func.get().arguments().argument().stream()
                            .filter(argumentContext -> argumentContext.name().getText().equals("name"))
                            .filter(argumentContext -> argumentContext.valueWithVariable().enumValue() != null)
                            .findFirst()
                            .map(argumentContext -> argumentContext.valueWithVariable().enumValue());

                    Optional<TerminalNode> field = func.get().arguments().argument().stream()
                            .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                            .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                            .findFirst()
                            .map(argumentContext -> argumentContext.valueWithVariable().StringValue());

                    if (name.isPresent() && field.isPresent()) {
                        Function function = new Function();
                        function.setName(functionEnumValueToFunctionName(name.get()));
                        function.setParameters(new ExpressionList(scalarFieldToExpression(table, functionFieldDefinitionContext.get())));
                        return function;
                    } else {
                        if (name.isEmpty()) {
                            throw new GraphQLProblem(FUNC_NAME_NOT_EXIST.bind(func.get().getText()));
                        } else {
                            throw new GraphQLProblem(FUNC_FIELD_NOT_EXIST.bind(func.get().getText()));
                        }
                    }
                }
            }
            return scalarFieldToExpression(table, fieldDefinitionContext);
        }
    }

    protected Expression scalarFieldToExpression(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (fieldTypeName.equals("Boolean")) {
            Function function = new Function();
            function.setName("IF");
            function.setParameters(new ExpressionList(Arrays.asList(fieldToColumn(table, fieldDefinitionContext), new HexValue("TRUE"), new HexValue("FALSE"))));
            return function;
        } else {
            return fieldToColumn(table, fieldDefinitionContext);
        }
    }

    protected String functionEnumValueToFunctionName(GraphqlParser.EnumValueContext enumValueContext) {
        switch (enumValueContext.enumValueName().getText()) {
            case "COUNT":
                return "COUNT";
            case "MAX":
                return "MAX";
            case "MIN":
                return "MIN";
            case "SUM":
                return "SUM";
            case "AVG":
                return "AVG";
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(enumValueContext.enumValueName().getText()));
    }

    protected StringValue selectionToStringValueKey(GraphqlParser.SelectionContext selectionContext) {
        return new StringValue(selectionContext.field().name().getText());
    }

    protected Table typeToTable(String typeName, int level) {
        if (manager.isQueryOperationType(typeName) || manager.isMutationOperationType(typeName)) {
            return dbNameUtil.dualTable();
        }
        return dbNameUtil.typeToTable(typeName, level);
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return dbNameUtil.fieldToColumn(table, objectFieldWithVariableContext);
    }

    protected Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return dbNameUtil.fieldToColumn(table, fieldDefinitionContext);
    }

    protected Column fieldToColumn(Table table, String fieldName) {
        return dbNameUtil.fieldToColumn(table, fieldName);
    }
}
