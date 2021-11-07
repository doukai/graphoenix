package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.common.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
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

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;
import static io.graphoenix.spi.error.GraphQLErrorType.*;

public class GraphqlQueryToSelect {

    private final IGraphqlDocumentManager manager;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlQueryToSelect(IGraphqlDocumentManager manager, GraphqlArgumentsToWhere argumentsToWhere) {
        this.manager = manager;
        this.argumentsToWhere = argumentsToWhere;
    }

    public List<String> createSelectsSql(String graphql) {
        return createSelectsSql(DOCUMENT_UTIL.graphqlToDocument(graphql));
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
            throw new GraphQLProblem().push(OPERATION_NOT_EXIST);
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
        throw new GraphQLProblem().push(QUERY_NOT_EXIST);
    }

    public Select objectSelectionToSelect(String typeName, List<GraphqlParser.SelectionContext> selectionContextList) {
        Select select = new Select();
        select.setSelectBody(objectSelectionToPlainSelect(typeName, selectionContextList));
        return select;
    }

    protected Stream<GraphqlParser.SelectionContext> fragmentUnzip(String typeName, GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.fragmentSpread() != null) {
            Optional<GraphqlParser.FragmentDefinitionContext> fragmentDefinitionContext = manager.getObjectFragmentDefinitionContext(typeName, selectionContext.fragmentSpread().fragmentName().getText());
            if (fragmentDefinitionContext.isPresent()) {
                return fragmentDefinitionContext.get().selectionSet().selection().stream();
            } else {
                throw new GraphQLProblem().push(FRAGMENT_NOT_EXIST.bind(typeName, selectionContext.fragmentSpread().fragmentName().getText()));
            }
        } else {
            return Stream.of(selectionContext);
        }
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
                                .flatMap(selectionContext -> fragmentUnzip(typeName, selectionContext))
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
        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

        if (manager.isQueryOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else if (manager.isMutationOperationType(typeName)) {
            selectExpressionItem.setAlias(new Alias("`data`"));
        } else {
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentTypeName, fieldDefinitionContext);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);

            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentTypeName, level - 1);
                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

                EqualsTo equalsParentColumn = new EqualsTo();
                equalsParentColumn.setLeftExpression(fieldToColumn(table, toFieldDefinition.get()));
                if (mapWithTypeArgument.isPresent()) {
                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinitionContext = manager.getObjectFieldDefinitionContext(mapWithTypeName.get(), mapWithFromFieldName.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinitionContext = manager.getObjectFieldDefinitionContext(mapWithTypeName.get(), mapWithToFieldName.get());
                        if (mapWithFromFieldDefinitionContext.isPresent() && mapWithToFieldDefinitionContext.isPresent()) {

                            Table withTable = typeToTable(mapWithTypeName.get(), level);
                            SubSelect selectWithTable = new SubSelect();
                            PlainSelect subPlainSelect = new PlainSelect();
                            subPlainSelect.setSelectItems(Collections.singletonList(new SelectExpressionItem(fieldToColumn(withTable, mapWithToFieldName.get()))));
                            EqualsTo equalsWithTableColumn = new EqualsTo();
                            equalsWithTableColumn.setLeftExpression(fieldToColumn(withTable, mapWithFromFieldName.get()));
                            equalsWithTableColumn.setRightExpression(fieldToColumn(parentTable, fromFieldDefinition.get()));
                            subPlainSelect.setWhere(equalsWithTableColumn);
                            subPlainSelect.setFromItem(withTable);
                            selectWithTable.setSelectBody(subPlainSelect);
                            equalsParentColumn.setRightExpression(selectWithTable);
                        } else {
                            GraphQLProblem graphQLProblem = new GraphQLProblem();
                            if (mapWithFromFieldDefinitionContext.isEmpty()) {
                                graphQLProblem.push(FIELD_NOT_EXIST.bind(mapWithTypeName.get(), mapWithFromFieldName.get()));
                            }
                            if (mapWithToFieldDefinitionContext.isEmpty()) {
                                graphQLProblem.push(FIELD_NOT_EXIST.bind(mapWithTypeName.get(), mapWithToFieldName.get()));
                            }
                            throw graphQLProblem;
                        }
                    } else {
                        GraphQLProblem graphQLProblem = new GraphQLProblem();
                        if (mapWithTypeName.isEmpty()) {
                            graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "with"));
                        }
                        if (mapWithFromFieldName.isEmpty()) {
                            graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "from"));
                        }
                        if (mapWithToFieldName.isEmpty()) {
                            graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "to"));
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
                    graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(parentTypeName, fieldDefinitionContext.name().getText()));
                }
                if (toFieldDefinition.isEmpty()) {
                    graphQLProblem.push(MAP_TO_FIELD_NOT_EXIST.bind(parentTypeName, fieldDefinitionContext.name().getText()));
                }
                throw graphQLProblem;
            }
        }
        return plainSelect;
    }

    protected Expression selectionToExpression(String typeName, GraphqlParser.SelectionContext selectionContext, int level) {
        return manager.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText())
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return jsonExtractFunction(objectSelectionToSubSelect(typeName, fieldTypeName, fieldDefinitionContext, selectionContext, level + 1));
                            } else {
                                return fieldToExpression(typeName, fieldDefinitionContext, level);
                            }
                        }
                ).orElseThrow(() -> {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(typeName, selectionContext.field().name().getText()));
                });
    }

    protected Function selectionToJsonFunction(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, ExpressionList expressionList) {

        Function function = new Function();
        function.setName("JSON_OBJECT");
        function.setParameters(expressionList);
        if (fieldDefinitionContext != null && manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            return jsonArrayFunction(new ExpressionList(function));
        }
        return function;
    }

    protected Function jsonArrayFunction(ExpressionList expressionList) {
        Function function = new Function();
        function.setName("JSON_ARRAYAGG");
        function.setParameters(expressionList);
        return function;
    }

    protected Function jsonExtractFunction(SubSelect subSelect) {
        Function function = new Function();
        function.setName("JSON_EXTRACT");
        function.setParameters(new ExpressionList(Arrays.asList(subSelect, new StringValue("$"))));
        return function;
    }

    protected SubSelect objectSelectionToSubSelect(String parentTypeName,
                                                   String typeName,
                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                   GraphqlParser.SelectionContext selectionContext
            , int level) {
        SubSelect subSelect = new SubSelect();
        PlainSelect plainSelect = objectSelectionToPlainSelect(parentTypeName, typeName, fieldDefinitionContext, selectionContext.field().selectionSet().selection(), level);

        if (manager.isMutationOperationType(parentTypeName)) {
            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);

            if (idFieldName.isPresent()) {
                Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), selectionContext.field().arguments())
                        .map(DB_VALUE_UTIL::createIdValueExpression)
                        .orElse(DB_VALUE_UTIL.createInsertIdUserVariable(typeName, idFieldName.get(), 0, 0));

                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(fieldToColumn(typeToTable(typeName, level), idFieldName.get()));
                idEqualsTo.setRightExpression(idValueExpression);
                plainSelect.setWhere(idEqualsTo);
            } else {
                throw new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
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
            } else {
                throw new GraphQLProblem(SELECTION_NOT_EXIST.bind(parentTypeName));
            }
        }
        subSelect.setSelectBody(plainSelect);
        return subSelect;
    }

    protected Expression fieldToExpression(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level) {
        if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(typeName, fieldDefinitionContext);
            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

            if (fromFieldDefinition.isPresent() && mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinitionContext = manager.getObjectFieldDefinitionContext(mapWithTypeName.get(), mapWithFromFieldName.get());
                    Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinitionContext = manager.getObjectFieldDefinitionContext(mapWithTypeName.get(), mapWithToFieldName.get());

                    if (mapWithFromFieldDefinitionContext.isPresent() && mapWithToFieldDefinitionContext.isPresent()) {
                        SubSelect subSelect = new SubSelect();
                        PlainSelect plainSelect = new PlainSelect();
                        Table witTable = typeToTable(mapWithTypeName.get(), level);
                        plainSelect.setFromItem(witTable);
                        SelectExpressionItem selectExpressionItem =
                                new SelectExpressionItem(
                                        jsonArrayFunction(
                                                new ExpressionList(
                                                        fieldToColumn(witTable, mapWithToFieldName.get())
                                                )
                                        )
                                );
                        plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));
                        EqualsTo equalsTo = new EqualsTo();
                        equalsTo.setLeftExpression(fieldToColumn(witTable, mapWithFromFieldName.get()));
                        equalsTo.setRightExpression(fieldToColumn(typeToTable(typeName, level), fromFieldDefinition.get()));
                        plainSelect.setWhere(equalsTo);
                        subSelect.setSelectBody(plainSelect);
                        return jsonExtractFunction(subSelect);
                    } else {
                        GraphQLProblem graphQLProblem = new GraphQLProblem();
                        if (mapWithFromFieldDefinitionContext.isEmpty()) {
                            graphQLProblem.push(FIELD_NOT_EXIST.bind(mapWithTypeName.get(), mapWithFromFieldName.get()));
                        }
                        if (mapWithToFieldDefinitionContext.isEmpty()) {
                            graphQLProblem.push(FIELD_NOT_EXIST.bind(mapWithTypeName.get(), mapWithToFieldName.get()));
                        }
                        throw graphQLProblem;
                    }
                } else {
                    GraphQLProblem graphQLProblem = new GraphQLProblem();
                    if (mapWithTypeName.isEmpty()) {
                        graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "with"));
                    }
                    if (mapWithFromFieldName.isEmpty()) {
                        graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "from"));
                    }
                    if (mapWithToFieldName.isEmpty()) {
                        graphQLProblem.push(FIELD_DIRECTIVE_ARGUMENT_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText(), "@map", "to"));
                    }
                    throw graphQLProblem;
                }
            } else {
                GraphQLProblem graphQLProblem = new GraphQLProblem();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(typeName, fieldDefinitionContext.name().getText()));
                }
                throw graphQLProblem;
            }
        } else {
            Table table = typeToTable(typeName, level);
            return fieldToColumn(table, fieldDefinitionContext);
        }
    }

    protected StringValue selectionToStringValueKey(GraphqlParser.SelectionContext selectionContext) {
        if (selectionContext.field().alias() != null) {
            return new StringValue(selectionContext.field().alias().getText());
        } else {
            return new StringValue(selectionContext.field().name().getText());
        }
    }

    protected Table typeToTable(String typeName) {
        if (manager.isQueryOperationType(typeName) || manager.isMutationOperationType(typeName)) {
            return DB_NAME_UTIL.dualTable();
        }
        return DB_NAME_UTIL.typeToTable(typeName);
    }

    protected Table typeToTable(String typeName, int level) {
        if (manager.isQueryOperationType(typeName) || manager.isMutationOperationType(typeName)) {
            return DB_NAME_UTIL.dualTable();
        }
        return DB_NAME_UTIL.typeToTable(typeName, level);
    }

    protected Column fieldToColumn(String typeName, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return DB_NAME_UTIL.fieldToColumn(typeToTable(typeName), fieldDefinitionContext);
    }

    protected Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return DB_NAME_UTIL.fieldToColumn(table, fieldDefinitionContext);
    }

    protected Column fieldToColumn(Table table, String fieldName) {
        return DB_NAME_UTIL.fieldToColumn(table, fieldName);
    }

    protected Column fieldToColumn(String typeName, String fieldName) {
        return DB_NAME_UTIL.fieldToColumn(typeToTable(typeName), fieldName);
    }
}
