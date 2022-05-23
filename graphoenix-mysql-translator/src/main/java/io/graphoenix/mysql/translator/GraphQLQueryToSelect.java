package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.IntValue;
import io.graphoenix.mysql.expression.JsonArrayAggregateFunction;
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
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_AGG_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.CONNECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.FUNC_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.FUNC_NAME_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OBJECT_SELECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.OPERATION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.SELECTION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
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
                throw new GraphQLErrors().add(OPERATION_NOT_EXIST);
            }
        }
        throw new GraphQLErrors().add(QUERY_NOT_EXIST);
    }

    public Stream<Tuple2<String, Select>> operationDefinitionToSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> queryOperationTypeDefinition = manager.getQueryOperationTypeDefinition();
            if (queryOperationTypeDefinition.isPresent()) {
                if (operationDefinitionContext.selectionSet() == null || operationDefinitionContext.selectionSet().selection().size() == 0) {
                    throw new GraphQLErrors(SELECTION_NOT_EXIST.bind(queryOperationTypeDefinition.get().getText()));
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
            throw new GraphQLErrors().add(QUERY_TYPE_NOT_EXIST);
        }
        throw new GraphQLErrors().add(QUERY_NOT_EXIST);
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
        plainSelect.setFromItem(table);

        Function function = jsonObjectFunction(
                new ExpressionList(
                        selectionContextList.stream()
                                .flatMap(subSelectionContext -> manager.fragmentUnzip(typeName, subSelectionContext))
                                .filter(subSelectionContext -> manager.isNotInvokeField(typeName, subSelectionContext.field().name().getText()))
                                .flatMap(subSelectionContext -> selectionToExpressionStream(typeName, subSelectionContext, level))
                                .collect(Collectors.toList())
                )
        );
        if (fieldDefinitionContext != null && manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            function = jsonArrayAggFunction(new ExpressionList(function), typeName, selectionContext, fieldDefinitionContext, level);
        }
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(function);
        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

        if (manager.isQueryOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (manager.isMutationOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (fieldDefinitionContext != null && !manager.isQueryOperationType(parentTypeName) && !manager.isMutationOperationType(parentTypeName) && !manager.isSubscriptionOperationType(parentTypeName)) {
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
                        Join join = new Join();
                        join.setLeft(true);
                        join.setRightItem(withTable);
                        EqualsTo joinEqualsTo = new EqualsTo();
                        joinEqualsTo.setLeftExpression(fieldToColumn(withTable, mapWithToFieldDefinition.get()));
                        joinEqualsTo.setRightExpression(fieldToColumn(table, toFieldDefinition.get()));

                        IsNullExpression isNotDeprecated = new IsNullExpression();
                        isNotDeprecated.setLeftExpression(fieldToColumn(withTable, DEPRECATED_FIELD_NAME));
                        join.addOnExpression(new MultiAndExpression(Arrays.asList(joinEqualsTo, isNotDeprecated)));
                        plainSelect.addJoins(join);
                        equalsParentColumn.setLeftExpression(fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                    } else {
                        GraphQLErrors graphQLErrors = new GraphQLErrors();
                        if (mapWithObjectDefinition.isEmpty()) {
                            graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                        }
                        if (mapWithFromFieldDefinition.isEmpty()) {
                            graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                        }
                        if (mapWithToFieldDefinition.isEmpty()) {
                            graphQLErrors.add(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                        }
                        throw graphQLErrors;
                    }
                } else {
                    equalsParentColumn.setLeftExpression(fieldToColumn(table, toFieldDefinition.get()));
                }
                equalsParentColumn.setRightExpression(fieldToColumn(parentTable, fromFieldDefinition.get()));
                plainSelect.setWhere(equalsParentColumn);
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (toFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLErrors;
            }
        }
        return plainSelect;
    }

    protected Stream<Expression> selectionToExpressionStream(String typeName, GraphqlParser.SelectionContext selectionContext, int level) {
        GraphqlParser.FieldDefinitionContext fieldDefinitionContext = manager.getObjectFieldDefinition(typeName, selectionContext.field().name().getText())
                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText())));

        if (manager.isConnectionField(typeName, fieldDefinitionContext.name().getText())) {
            if (selectionContext.field().selectionSet() == null || selectionContext.field().selectionSet().selection().size() == 0) {
                throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(selectionContext.getText()));
            }
            Optional<GraphqlParser.DirectiveContext> connection = fieldDefinitionContext.directives().directive().stream()
                    .filter(directiveContext -> directiveContext.name().getText().equals(CONNECTION_DIRECTIVE_NAME))
                    .findFirst();

            if (connection.isPresent()) {
                Optional<String> connectionFieldName = connection.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                Optional<String> connectionAggFieldName = connection.get().arguments().argument().stream()
                        .filter(argumentContext -> argumentContext.name().getText().equals("agg"))
                        .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                        .findFirst()
                        .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));

                if (connectionFieldName.isPresent() && connectionAggFieldName.isPresent()) {
                    GraphqlParser.FieldDefinitionContext connectionFieldDefinitionContext = manager.getField(typeName, connectionFieldName.get())
                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(typeName, connectionFieldName.get())));
                    GraphqlParser.FieldDefinitionContext connectionAggFieldDefinitionContext = manager.getField(typeName, connectionAggFieldName.get())
                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(typeName, connectionAggFieldName.get())));
                    String connectionFieldTypeName = manager.getFieldTypeName(connectionFieldDefinitionContext.type());

                    return Stream.concat(
                            buildConnectionSelection(selectionContext, connectionFieldDefinitionContext).stream()
                                    .flatMap(connectionSelectionContext ->
                                            Stream.of(
                                                    fieldDefinitionToStringValueKey(connectionFieldDefinitionContext),
                                                    jsonExtractFunction(objectSelectionToSubSelect(typeName, connectionFieldTypeName, connectionFieldDefinitionContext, connectionSelectionContext, level + 1), true)
                                            )
                                    ),
                            buildConnectionTotalSelection(selectionContext, connectionAggFieldDefinitionContext).stream()
                                    .flatMap(aggSelectionContext ->
                                            Stream.of(
                                                    fieldDefinitionToStringValueKey(connectionAggFieldDefinitionContext),
                                                    jsonExtractFunction(objectSelectionToSubSelect(typeName, connectionFieldTypeName, connectionAggFieldDefinitionContext, aggSelectionContext, level + 1), false)
                                            )
                                    )
                    );
                } else {
                    if (connectionFieldName.isEmpty()) {
                        throw new GraphQLErrors(CONNECTION_FIELD_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
                    } else {
                        throw new GraphQLErrors(CONNECTION_AGG_FIELD_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
                    }
                }
            }
            throw new GraphQLErrors(CONNECTION_NOT_EXIST.bind(fieldDefinitionContext.name().getText()));
        } else {
            StringValue selectionKey = selectionToStringValueKey(selectionContext);
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            if (manager.isObject(fieldTypeName)) {
                return Stream.of(
                        selectionKey,
                        jsonExtractFunction(objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext, level + 1), manager.fieldTypeIsList(fieldDefinitionContext.type()))
                );
            } else {
                return Stream.of(
                        selectionKey,
                        fieldToExpression(typeName, fieldDefinitionContext, selectionContext, level)
                );
            }
        }
    }

    protected Optional<GraphqlParser.SelectionContext> buildConnectionSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext connectionFieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(connectionFieldDefinitionContext.type());
        return selectionContext.field().selectionSet().selection().stream()
                .filter(subSelectionContext -> subSelectionContext.field().name().getText().equals("edges"))
                .findFirst()
                .map(edges -> {
                            if (edges.field().selectionSet() == null || edges.field().selectionSet().selection().size() == 0) {
                                throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(edges.getText()));
                            }
                            return Stream.concat(
                                    edges.field().selectionSet().selection().stream()
                                            .filter(subSelectionContext -> subSelectionContext.field().name().getText().equals("cursor"))
                                            .findFirst()
                                            .map(cursor ->
                                                    new Field(
                                                            manager.getFieldByDirective(fieldTypeName, "cursor")
                                                                    .findFirst()
                                                                    .or(() -> manager.getObjectTypeIDFieldDefinition(fieldTypeName))
                                                                    .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName)))
                                                                    .name()
                                                                    .getText()
                                                    )
                                            )
                                            .stream(),
                                    edges.field().selectionSet().selection().stream()
                                            .filter(subSelectionContext -> subSelectionContext.field().name().getText().equals("node"))
                                            .flatMap(node -> {
                                                        if (node.field().selectionSet() == null || node.field().selectionSet().selection().size() == 0) {
                                                            throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(node.getText()));
                                                        }
                                                        return node.field().selectionSet().selection().stream().map(Field::new);
                                                    }
                                            )
                            );
                        }
                )
                .map(fieldStream -> {
                            Field field = new Field(connectionFieldDefinitionContext.name().getText())
                                    .setFields(fieldStream.collect(Collectors.toCollection(LinkedHashSet::new)));
                            if (selectionContext.field().arguments() != null && selectionContext.field().arguments().argument().size() > 0) {
                                Set<Argument> arguments = selectionContext.field().arguments().argument().stream()
                                        .map(argumentContext -> {
                                                    if (argumentContext.name().getText().equals(FIRST_INPUT_NAME) || argumentContext.name().getText().equals(LAST_INPUT_NAME)) {
                                                        return new Argument()
                                                                .setName(argumentContext.name().getText())
                                                                .setValueWithVariable(new IntValue(Integer.parseInt(argumentContext.valueWithVariable().IntValue().getText()) + 1));
                                                    } else {
                                                        return new Argument()
                                                                .setName(argumentContext.name().getText())
                                                                .setValueWithVariable(argumentContext.valueWithVariable());
                                                    }
                                                }
                                        )
                                        .collect(Collectors.toCollection(LinkedHashSet::new));
                                field.setArguments(arguments);
                            }
                            return DOCUMENT_UTIL.graphqlToSelection(field.toString());
                        }
                );
    }

    protected Optional<GraphqlParser.SelectionContext> buildConnectionTotalSelection(GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext connectionAggFieldDefinitionContext) {
        String fieldTypeName = manager.getFieldTypeName(connectionAggFieldDefinitionContext.type());
        Optional<GraphqlParser.SelectionContext> totalCount = selectionContext.field().selectionSet().selection().stream()
                .filter(subSelectionContext -> subSelectionContext.field().name().getText().equals("totalCount"))
                .findFirst();
        Field field = new Field(connectionAggFieldDefinitionContext.name().getText())
                .addField(new Field(manager.getObjectTypeIDFieldName(fieldTypeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName))).concat("Count")));

        if (selectionContext.field().arguments() != null && selectionContext.field().arguments().argument().size() > 0) {
            LinkedHashSet<Argument> arguments = selectionContext.field().arguments().argument().stream()
                    .filter(argumentContext -> !argumentContext.name().getText().equals(FIRST_INPUT_NAME) && !argumentContext.name().getText().equals(LAST_INPUT_NAME))
                    .map(Argument::new)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            field.setArguments(arguments);
        }

        if (totalCount.isPresent()) {
            return Optional.of(
                    DOCUMENT_UTIL.graphqlToSelection(field.toString())
            );
        }
        return Optional.empty();
    }

    protected Function jsonObjectFunction(ExpressionList expressionList) {
        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(expressionList);
        return function;
    }

    protected Function jsonArrayAggFunction(ExpressionList expressionList, String typeName, GraphqlParser.SelectionContext selectionContext, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level) {
        JsonArrayAggregateFunction jsonArrayAggregateFunction = new JsonArrayAggregateFunction();
        jsonArrayAggregateFunction.setName("JSON_ARRAYAGG");
        jsonArrayAggregateFunction.setParameters(expressionList);
        buildSortArguments(jsonArrayAggregateFunction, typeName, fieldDefinitionContext, selectionContext, level);
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
            throw new GraphQLErrors(OBJECT_SELECTION_NOT_EXIST.bind(selectionContext.getText()));
        }
        PlainSelect plainSelect = objectSelectionToPlainSelect(parentTypeName, typeName, selectionContext, fieldDefinitionContext, selectionContext.field().selectionSet().selection(), level);
        buildCursorArguments(plainSelect, typeName, selectionContext, fieldDefinitionContext, level);

        if (manager.isMutationOperationType(parentTypeName)) {
            Optional<Expression> whereExpression = argumentsToWhere.objectValueWithVariableToWhereExpression(fieldDefinitionContext, selectionContext.field().arguments());
            if (whereExpression.isPresent()) {
                plainSelect.setWhere(whereExpression.get());
            } else {
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
                    throw new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
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
        if (!manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            Limit limit = new Limit();
            limit.setOffset(new LongValue(0));
            limit.setRowCount(new LongValue(1));
            plainSelect.setLimit(limit);
        }
        subSelect.setSelectBody(plainSelect);
        return subSelect;
    }

    protected void buildPageArguments(JsonArrayAggregateFunction jsonArrayAggregateFunction, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext) {
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            Optional<GraphqlParser.InputValueDefinitionContext> firstInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(FIRST_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> lastInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(LAST_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.InputValueDefinitionContext> offsetInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(OFFSET_INPUT_NAME))
                    .findFirst();

            Optional<GraphqlParser.ArgumentContext> first = firstInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
            Optional<GraphqlParser.ArgumentContext> last = lastInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
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

            if (last.isPresent()) {
                Limit limit = new Limit();
                limit.setRowCount(dbValueUtil.valueWithVariableToDBValue(last.get().valueWithVariable()));
                offset.ifPresent(argumentContext -> limit.setOffset(dbValueUtil.valueWithVariableToDBValue(argumentContext.valueWithVariable())));
                jsonArrayAggregateFunction.setLimit(limit);
            } else {
                if (lastInput.isPresent() && lastInput.get().defaultValue() != null) {
                    Limit limit = new Limit();
                    limit.setRowCount(dbValueUtil.valueToDBValue(lastInput.get().defaultValue().value()));
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
                .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

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

    protected void buildSortArguments(JsonArrayAggregateFunction jsonArrayAggregateFunction, String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.SelectionContext selectionContext, int level) {
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            Table table = typeToTable(typeName, level);

            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                Optional<GraphqlParser.InputValueDefinitionContext> orderByInput = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(ORDER_BY_INPUT_NAME))
                        .findFirst();

                Optional<GraphqlParser.ArgumentContext> orderBy = orderByInput.flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(selectionContext.field().arguments(), inputValueDefinitionContext));
                orderBy.ifPresent(argumentContext -> {
                            List<OrderByElement> orderByElementList = argumentContext.valueWithVariable().objectValueWithVariable().objectFieldWithVariable().stream()
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
                                    .collect(Collectors.toList());
                            jsonArrayAggregateFunction.setOrderByElements(orderByElementList);
                        }
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

            if (jsonArrayAggregateFunction.getOrderByElements() == null || jsonArrayAggregateFunction.getOrderByElements().size() == 0) {
                if (selectionContext.field().arguments() != null && selectionContext.field().arguments().argument().size() > 0) {
                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    Optional<GraphqlParser.ArgumentContext> lastArgument = selectionContext.field().arguments().argument().stream()
                            .filter(argumentContext -> argumentContext.name().getText().equals(LAST_INPUT_NAME))
                            .findFirst();
                    OrderByElement orderByElement = new OrderByElement();
                    orderByElement.setAsc(lastArgument.isEmpty());
                    GraphqlParser.FieldDefinitionContext cursorFieldDefinitionContext = manager.getFieldByDirective(fieldTypeName, "cursor").findFirst()
                            .or(() -> manager.getObjectTypeIDFieldDefinition(fieldTypeName))
                            .orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName)));
                    orderByElement.setExpression(fieldToColumn(table, cursorFieldDefinitionContext));
                    jsonArrayAggregateFunction.setOrderByElements(Collections.singletonList(orderByElement));
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
                    .filter(directiveContext -> directiveContext.name().getText().equals(FUNC_DIRECTIVE_NAME))
                    .flatMap(directiveContext -> directiveContext.arguments().argument().stream()
                            .filter(argumentContext -> argumentContext.name().getText().equals("field"))
                            .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                            .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue())))
                    .findFirst()
                    .map(fieldName -> manager.getField(typeName, fieldName).orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(typeName, fieldName))));
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
                            .filter(directiveContext -> directiveContext.name().getText().equals(FUNC_DIRECTIVE_NAME))
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
                                throw new GraphQLErrors(FUNC_NAME_NOT_EXIST.bind(func.get().getText()));
                            } else {
                                throw new GraphQLErrors(FUNC_FIELD_NOT_EXIST.bind(func.get().getText()));
                            }
                        }
                    } else {
                        function = jsonArrayAggFunction(
                                new ExpressionList(scalarFieldToExpression(withTable, mapWithToFieldDefinition.get())),
                                mapWithObjectDefinition.get().name().getText(),
                                selectionContext,
                                mapWithToFieldDefinition.get(),
                                level
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
                    IsNullExpression isNotDeprecated = new IsNullExpression();
                    isNotDeprecated.setLeftExpression(fieldToColumn(withTable, DEPRECATED_FIELD_NAME));
                    plainSelect.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, isNotDeprecated)));
                    subSelect.setSelectBody(plainSelect);
                    return jsonExtractFunction(subSelect, true);
                } else {
                    GraphQLErrors graphQLErrors = new GraphQLErrors();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithToFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLErrors;
                }
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLErrors;
            }
        } else {
            Table table = typeToTable(typeName, level);
            if (functionFieldDefinitionContext.isPresent()) {
                Optional<GraphqlParser.DirectiveContext> func = fieldDefinitionContext.directives().directive().stream()
                        .filter(directiveContext -> directiveContext.name().getText().equals(FUNC_DIRECTIVE_NAME))
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
                            throw new GraphQLErrors(FUNC_NAME_NOT_EXIST.bind(func.get().getText()));
                        } else {
                            throw new GraphQLErrors(FUNC_FIELD_NOT_EXIST.bind(func.get().getText()));
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
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(enumValueContext.enumValueName().getText()));
    }

    protected StringValue selectionToStringValueKey(GraphqlParser.SelectionContext selectionContext) {
        return new StringValue(selectionContext.field().name().getText());
    }

    protected StringValue fieldDefinitionToStringValueKey(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new StringValue(fieldDefinitionContext.name().getText());
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
