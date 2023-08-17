package io.graphoenix.mysql.translator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.mysql.translator.expression.JsonTable;
import io.graphoenix.mysql.translator.utils.DBNameUtil;
import io.graphoenix.mysql.translator.utils.DBValueUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.constant.Hammurabi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.ID_ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.INPUT_OBJECT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_FIELD_NAME;
import static io.graphoenix.spi.constant.Hammurabi.EXCLUDE_INPUT;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

@ApplicationScoped
public class GraphQLMutationToStatements {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final IGraphQLFieldMapManager mapper;
    private final GraphQLQueryToSelect graphQLQueryToSelect;
    private final GraphQLArgumentsToWhere graphQLArgumentsToWhere;
    private final DBNameUtil dbNameUtil;
    private final DBValueUtil dbValueUtil;

    @Inject
    public GraphQLMutationToStatements(IGraphQLDocumentManager manager, PackageManager packageManager, IGraphQLFieldMapManager mapper, GraphQLQueryToSelect graphQLQueryToSelect, GraphQLArgumentsToWhere graphQLArgumentsToWhere, DBNameUtil dbNameUtil, DBValueUtil dbValueUtil) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.mapper = mapper;
        this.graphQLQueryToSelect = graphQLQueryToSelect;
        this.graphQLArgumentsToWhere = graphQLArgumentsToWhere;
        this.dbNameUtil = dbNameUtil;
        this.dbValueUtil = dbValueUtil;
    }

    public Stream<String> createStatementsSQL(String graphql) {
        return operationDefinitionToStatementStream(DOCUMENT_UTIL.graphqlToOperation(graphql)).map(Object::toString);
    }

    public Stream<Statement> createStatements(String graphql) {
        return operationDefinitionToStatementStream(DOCUMENT_UTIL.graphqlToOperation(graphql));
    }

    public Stream<String> createStatementsSQL(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToStatementStream(operationDefinitionContext).map(Object::toString);
    }

    public Stream<Statement> createStatements(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return operationDefinitionToStatementStream(operationDefinitionContext);
    }

    protected Stream<Statement> operationDefinitionToStatementStream(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() != null && operationDefinitionContext.operationType().MUTATION() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = manager.getMutationOperationTypeDefinition();
            if (mutationOperationTypeDefinition.isPresent()) {
                String mutationTypeName = mutationOperationTypeDefinition.get().typeName().name().getText();
                List<GraphqlParser.SelectionContext> selectionContextList = operationDefinitionContext.selectionSet().selection().stream()
                        .filter(selectionContext -> packageManager.isLocalPackage(mutationTypeName, selectionContext.field().name().getText()))
                        .filter(selectionContext -> manager.isNotInvokeField(mutationTypeName, selectionContext.field().name().getText()))
                        .filter(selectionContext -> manager.isNotFetchField(mutationTypeName, selectionContext.field().name().getText()))
                        .filter(selectionContext -> manager.isNotFunctionField(mutationTypeName, selectionContext.field().name().getText()))
                        .filter(selectionContext -> manager.isNotConnectionField(mutationTypeName, selectionContext.field().name().getText()))
                        .collect(Collectors.toList());
                if (selectionContextList.size() == 0) {
                    return Stream.empty();
                }
                return Stream.concat(
                        selectionContextList.stream().flatMap(this::selectionToStatementStream),
                        Stream.of(graphQLQueryToSelect.objectSelectionToSelect(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()))
                );
            }
            throw new GraphQLErrors(MUTATION_TYPE_NOT_EXIST);
        }
        throw new GraphQLErrors(MUTATION_NOT_EXIST);
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getObjectFieldDefinition(mutationTypeName, selectionContext.field().name().getText()));

        if (mutationFieldTypeDefinitionContext.isPresent()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = mutationFieldTypeDefinitionContext.get();
            Optional<GraphqlParser.InputValueDefinitionContext> listInputValueDefinition = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(LIST_INPUT_NAME))
                    .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                    .findFirst();

            GraphqlParser.ArgumentsContext argumentsContext = selectionContext.field().arguments();
            Optional<GraphqlParser.ArgumentContext> listArgument = argumentsContext.argument().stream()
                    .filter(argumentContext -> argumentContext.name().getText().equals(LIST_INPUT_NAME))
                    .findFirst();

            if (listInputValueDefinition.isPresent() && listArgument.isPresent()) {
                Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(listInputValueDefinition.get().type()));
                if (inputObjectTypeDefinitionContext.isPresent()) {
                    if (listArgument.get().valueWithVariable().variable() != null) {
                        return arrayObjectVariableToInsert(
                                fieldDefinitionContext,
                                inputObjectTypeDefinitionContext.get(),
                                listArgument.get().valueWithVariable()
                        ).map(insert -> insert);
                    } else if (listArgument.get().valueWithVariable().arrayValueWithVariable() != null) {
                        List<GraphqlParser.ValueWithVariableContext> valueWithVariableContexts = listArgument.get().valueWithVariable().arrayValueWithVariable().valueWithVariable();
                        return IntStream.range(0, valueWithVariableContexts.size())
                                .filter(index -> valueWithVariableContexts.get(index).objectValueWithVariable() != null)
                                .mapToObj(index -> objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext.get(), valueWithVariableContexts.get(index).objectValueWithVariable(), index))
                                .flatMap(statementStream -> statementStream);
                    } else {
                        throw new GraphQLErrors(ARGUMENT_NOT_EXIST.bind(LIST_INPUT_NAME));
                    }
                } else {
                    throw new GraphQLErrors(INPUT_OBJECT_NOT_EXIST.bind(manager.getInputObject(manager.getFieldTypeName(listInputValueDefinition.get().type()))));
                }
            } else {
                return argumentsToStatementStream(fieldDefinitionContext, selectionContext, argumentsContext, manager.getMutationType(selectionContext));
            }
        }
        throw new GraphQLErrors(MUTATION_NOT_EXIST);
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.SelectionContext selectionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext,
                                                           Hammurabi.MutationType mutationType) {

        Stream<Statement> insertStatementStream = argumentsToInsertStatementStream(fieldDefinitionContext, argumentsContext, mutationType);

        Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext).flatMap(dbValueUtil::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, 0, 0));

        Stream<Statement> objectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .map(argumentContext -> {
                                                                            if (argumentContext.valueWithVariable().variable() != null) {
                                                                                return objectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        argumentContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return objectValueWithVariableToInsertStatementStream(
                                                                                        fieldDefinitionContext,
                                                                                        idValueExpression,
                                                                                        subFieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        argumentContext.valueWithVariable().objectValueWithVariable(),
                                                                                        mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        mapper.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable())
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        0,
                                                                                        0
                                                                                );
                                                                            }
                                                                        }
                                                                ).orElseGet(() ->
                                                                objectDefaultValueToStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        inputObjectTypeDefinitionContext,
                                                                        inputValueDefinitionContext,
                                                                        mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                        0,
                                                                        0
                                                                )
                                                        )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .map(argumentContext -> {
                                                                            if (argumentContext.valueWithVariable().variable() != null) {
                                                                                return arrayObjectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        argumentContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return manager.mergeToList(selectionContext, argumentContext.name().getText()) ?
                                                                                        listObjectValueWithVariableToMergeStatementStream(
                                                                                                fieldDefinitionContext,
                                                                                                idValueExpression,
                                                                                                subFieldDefinitionContext,
                                                                                                inputObjectTypeDefinitionContext,
                                                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                                0
                                                                                        ) :
                                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                                fieldDefinitionContext,
                                                                                                idValueExpression,
                                                                                                subFieldDefinitionContext,
                                                                                                inputObjectTypeDefinitionContext,
                                                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                                0
                                                                                        );
                                                                            }
                                                                        }
                                                                )
                                                                .orElseGet(() ->
                                                                        listObjectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                .map(argumentContext -> {
                                                            if (argumentContext.valueWithVariable().variable() != null) {
                                                                return arrayVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        argumentContext.valueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            } else {
                                                                return listValueWithVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            }
                                                        }
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                )
                );

        return Stream.concat(insertStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream)));
    }

    protected Stream<Statement> objectValueWithVariableToInsertStatementStream(
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
            GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
            GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
            int index) {

        if (objectValueWithVariableContext == null) {
            return Stream.empty();
        }

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);

        Expression idValueExpression = objectIdFieldWithVariableContext.flatMap(dbValueUtil::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, 0, index));

        Stream<Statement> insertStatementStream = objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueWithVariableContext, 0, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext -> {
                                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                                return objectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return objectValueWithVariableToInsertStatementStream(
                                                                                        fieldDefinitionContext,
                                                                                        idValueExpression,
                                                                                        subFieldDefinitionContext,
                                                                                        subInputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        mapper.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable())
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        0,
                                                                                        index
                                                                                );
                                                                            }
                                                                        }
                                                                )
                                                                .orElseGet(() ->
                                                                        objectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0,
                                                                                index
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> listObjectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext -> {
                                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                                return arrayObjectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return listObjectValueWithVariableToInsertStatementStream(
                                                                                        fieldDefinitionContext,
                                                                                        idValueExpression,
                                                                                        subFieldDefinitionContext,
                                                                                        subInputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        0
                                                                                );
                                                                            }
                                                                        }
                                                                )
                                                                .orElseGet(() ->
                                                                        listObjectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );


        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext -> {
                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                return arrayVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        objectFieldWithVariableContext.valueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            } else {
                                                                return listValueWithVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            }
                                                        }
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                )
                );

        return Stream.concat(insertStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream)));
    }

    protected Stream<Statement> objectValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               Expression parentIdValueExpression,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               Expression fromValueExpression,
                                                                               Expression toValueExpression,
                                                                               int level,
                                                                               int index) {

        if (objectValueWithVariableContext == null) {
            Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    fromValueExpression
            );
            Stream<Statement> objectTypeFieldRelationRemoveStream = objectTypeFieldRelationRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    null,
                    fromValueExpression
            );
            return Stream.concat(objectWithTypeRemoveStream, objectTypeFieldRelationRemoveStream);
        }

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);

        Expression idValueExpression = objectIdFieldWithVariableContext.flatMap(dbValueUtil::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> insertStatementStream = objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueWithVariableContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext -> {
                                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                                return objectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return objectValueWithVariableToInsertStatementStream(
                                                                                        fieldDefinitionContext,
                                                                                        idValueExpression,
                                                                                        subFieldDefinitionContext,
                                                                                        subInputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        mapper.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable())
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        level + 1,
                                                                                        index
                                                                                );
                                                                            }
                                                                        }
                                                                )
                                                                .orElseGet(() ->
                                                                        objectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> objectTypeFieldRelationStatementStream = objectTypeFieldRelationStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpression,
                fromValueExpression,
                toValueExpression
        );

        Stream<Statement> listObjectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext -> {
                                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                                return arrayObjectVariableToInsert(
                                                                                        fieldDefinitionContext,
                                                                                        inputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable()
                                                                                );
                                                                            } else {
                                                                                return listObjectValueWithVariableToInsertStatementStream(
                                                                                        fieldDefinitionContext,
                                                                                        idValueExpression,
                                                                                        subFieldDefinitionContext,
                                                                                        subInputObjectTypeDefinitionContext,
                                                                                        objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                        level + 1
                                                                                );
                                                                            }
                                                                        }
                                                                )
                                                                .orElseGet(() ->
                                                                        listObjectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext -> {
                                                            if (objectFieldWithVariableContext.valueWithVariable().variable() != null) {
                                                                return arrayVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        objectFieldWithVariableContext.valueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            } else {
                                                                return listValueWithVariableToInsertStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                        mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                                );
                                                            }
                                                        }
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                )
                );

        return Stream.concat(insertStatementStream, Stream.concat(objectTypeFieldRelationStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
    }

    protected Stream<Statement> objectValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                             Expression parentIdValueExpression,
                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                             GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                             GraphqlParser.ObjectValueContext objectValueContext,
                                                             Expression fromValueExpression,
                                                             Expression toValueExpression,
                                                             int level,
                                                             int index) {

        if (objectValueContext == null) {
            Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    fromValueExpression
            );
            Stream<Statement> objectTypeFieldRelationRemoveStream = objectTypeFieldRelationRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    null,
                    fromValueExpression
            );
            return Stream.concat(objectWithTypeRemoveStream, objectTypeFieldRelationRemoveStream);
        }

        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);

        Expression idValueExpression = objectIdFieldContext.flatMap(dbValueUtil::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> insertStatementStream = objectValueToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                                .map(objectFieldContext ->
                                                                        objectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldContext.value().objectValue(),
                                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                                                mapper.getMapToValueFromObjectField(subFieldDefinitionContext, objectFieldContext.value().objectValue())
                                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                                .orElseGet(() ->
                                                                        objectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> objectTypeFieldRelationStatementStream = objectTypeFieldRelationStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpression,
                fromValueExpression,
                toValueExpression
        );

        Stream<Statement> listObjectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                                .map(objectFieldContext ->
                                                                        listObjectValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldContext.value().arrayValue(),
                                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                                                level + 1
                                                                        )
                                                                )
                                                                .orElseGet(() ->
                                                                        listObjectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                                .orElseThrow(() -> new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                )
                );

        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext).stream()
                                .filter(manager::isNotFetchField)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                .map(objectFieldContext ->
                                                        listValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                objectFieldContext.value().arrayValue(),
                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null)
                                                        )
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null)
                                                        )
                                                )
                                )
                );

        return Stream.concat(insertStatementStream, Stream.concat(objectTypeFieldRelationStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
    }

    protected Stream<Statement> objectDefaultValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                    Expression parentIdValueExpression,
                                                                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                    GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                    GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext,
                                                                    Expression fromValueExpression,
                                                                    int level,
                                                                    int index) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                return objectValueToStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        parentInputValueDefinitionContext.defaultValue().value().objectValue(),
                        fromValueExpression,
                        mapper.getMapToValueFromObjectField(fieldDefinitionContext, parentInputValueDefinitionContext.defaultValue().value().objectValue())
                                .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                        level,
                        index
                );
            }
//            else {
//                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
//            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listObjectValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                   Expression parentIdValueExpression,
                                                                                   GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                   GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                                   Expression fromValueExpression,
                                                                                   int level) {

        Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listObjectValueInsertStatementStream =
                arrayValueWithVariableContext == null ?
                        Stream.empty() :
                        IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                                .mapToObj(index ->
                                        objectValueWithVariableToInsertStatementStream(
                                                parentFieldDefinitionContext,
                                                parentIdValueExpression,
                                                fieldDefinitionContext,
                                                inputObjectTypeDefinitionContext,
                                                arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable(),
                                                fromValueExpression,
                                                mapper.getMapToValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())
                                                        .map(dbValueUtil::scalarValueWithVariableToDBValue).orElse(null),
                                                level,
                                                index
                                        )
                                )
                                .flatMap(statementStream -> statementStream);

        List<Expression> idValueExpressionList =
                arrayValueWithVariableContext == null ?
                        null :
                        IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                                .mapToObj(index ->
                                        manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())
                                                .flatMap(dbValueUtil::createIdValueExpression)
                                                .orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index))
                                )
                                .collect(Collectors.toList());

        Stream<Statement> objectTypeFieldRelationRemoveStream = objectTypeFieldRelationRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpressionList,
                fromValueExpression
        );

        return Stream.concat(objectWithTypeRemoveStream, Stream.concat(listObjectValueInsertStatementStream, objectTypeFieldRelationRemoveStream));
    }

    protected Stream<Statement> listObjectValueWithVariableToMergeStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                                  Expression parentIdValueExpression,
                                                                                  GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                  GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                                  GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                                  Expression fromValueExpression,
                                                                                  int level) {

        Stream<Statement> objectWithTypeRemoveStream = Stream.empty();
        Stream<Statement> listObjectValueInsertStatementStream = Stream.empty();
        Stream<Statement> objectTypeFieldRelationRemoveStream = Stream.empty();

        List<GraphqlParser.ValueWithVariableContext> idValueWithVariableList = Stream.ofNullable(arrayValueWithVariableContext.valueWithVariable())
                .flatMap(Collection::stream)
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .flatMap(valueWithVariableContext -> manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), valueWithVariableContext.objectValueWithVariable()).stream())
                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                .collect(Collectors.toList());

        List<GraphqlParser.ValueWithVariableContext> mergeValueWithVariableList = Stream.ofNullable(arrayValueWithVariableContext.valueWithVariable())
                .flatMap(Collection::stream)
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .filter((valueWithVariableContext -> manager.getIsDeprecatedObjectFieldWithVariable(fieldDefinitionContext.type(), valueWithVariableContext.objectValueWithVariable()).isEmpty()))
                .collect(Collectors.toList());

        List<GraphqlParser.ValueWithVariableContext> removeIDValueWithVariableList = Stream.ofNullable(arrayValueWithVariableContext.valueWithVariable())
                .flatMap(Collection::stream)
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .filter((valueWithVariableContext -> manager.getIsDeprecatedObjectFieldWithVariable(fieldDefinitionContext.type(), valueWithVariableContext.objectValueWithVariable()).isPresent()))
                .map(valueWithVariableContext ->
                        manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), valueWithVariableContext.objectValueWithVariable())
                                .orElseThrow(() -> new GraphQLErrors(ID_ARGUMENT_NOT_EXIST.bind(valueWithVariableContext.objectValueWithVariable().getText())))
                )
                .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable)
                .collect(Collectors.toList());

        if (!idValueWithVariableList.isEmpty()) {
            objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    fromValueExpression,
                    idValueWithVariableList
            );
        }

        if (!mergeValueWithVariableList.isEmpty()) {
            listObjectValueInsertStatementStream = IntStream.range(0, mergeValueWithVariableList.size())
                    .mapToObj(index ->
                            objectValueWithVariableToInsertStatementStream(
                                    parentFieldDefinitionContext,
                                    parentIdValueExpression,
                                    fieldDefinitionContext,
                                    inputObjectTypeDefinitionContext,
                                    mergeValueWithVariableList.get(index).objectValueWithVariable(),
                                    fromValueExpression,
                                    mapper.getMapToValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, mergeValueWithVariableList.get(index).objectValueWithVariable())
                                            .map(dbValueUtil::scalarValueWithVariableToDBValue)
                                            .orElse(null),
                                    level,
                                    index
                            )
                    )
                    .flatMap(statementStream -> statementStream);
        }

        if (!removeIDValueWithVariableList.isEmpty()) {
            objectTypeFieldRelationRemoveStream = objectTypeFieldRelationRemoveStream(
                    parentFieldDefinitionContext,
                    parentIdValueExpression,
                    fieldDefinitionContext,
                    fromValueExpression,
                    removeIDValueWithVariableList
            );
        }
        return Stream.concat(objectWithTypeRemoveStream, Stream.concat(listObjectValueInsertStatementStream, objectTypeFieldRelationRemoveStream));
    }

    protected Stream<Statement> listObjectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                       GraphqlParser.ArrayValueContext arrayValueContext,
                                                                       Expression fromValueExpression,
                                                                       int level) {

        Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listObjectValueInsertStatementStream =
                arrayValueContext == null ?
                        Stream.empty() :
                        IntStream.range(0, arrayValueContext.value().size())
                                .mapToObj(index ->
                                        objectValueToStatementStream(
                                                parentFieldDefinitionContext,
                                                parentIdValueExpression,
                                                fieldDefinitionContext,
                                                inputObjectTypeDefinitionContext,
                                                arrayValueContext.value(index).objectValue(),
                                                fromValueExpression,
                                                mapper.getMapToValueFromObjectField(fieldDefinitionContext, arrayValueContext.value(index).objectValue())
                                                        .map(dbValueUtil::scalarValueToDBValue).orElse(null),
                                                level,
                                                index
                                        )
                                )
                                .flatMap(statementStream -> statementStream);

        List<Expression> idValueExpressionList =
                arrayValueContext == null ?
                        null :
                        IntStream.range(0, arrayValueContext.value().size())
                                .mapToObj(index ->
                                        manager.getIDObjectField(fieldDefinitionContext.type(), arrayValueContext.value(index).objectValue())
                                                .flatMap(dbValueUtil::createIdValueExpression)
                                                .orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index))
                                )
                                .collect(Collectors.toList());

        Stream<Statement> objectTypeFieldRelationRemoveStream = objectTypeFieldRelationRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpressionList,
                fromValueExpression
        );

        return Stream.concat(objectWithTypeRemoveStream, Stream.concat(listObjectValueInsertStatementStream, objectTypeFieldRelationRemoveStream));
    }

    protected Stream<Statement> listObjectDefaultValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                        Expression parentIdValueExpression,
                                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                        GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                        GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext,
                                                                        Expression fromValueExpression,
                                                                        int level) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                return listObjectValueToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        parentInputValueDefinitionContext.defaultValue().value().arrayValue(),
                        fromValueExpression,
                        level
                );
            }
//            else {
//                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
//            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                             Expression parentIdValueExpression,
                                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                             Expression fromValueExpression) {

        Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listValueInsertStatementStream =
                arrayValueWithVariableContext == null ?
                        Stream.empty() :
                        IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                                .mapToObj(index ->
                                        scalarOrEnumTypeFieldRelationInsertStream(
                                                parentFieldDefinitionContext,
                                                parentIdValueExpression,
                                                fieldDefinitionContext,
                                                dbValueUtil.valueWithVariableToDBValue(arrayValueWithVariableContext.valueWithVariable(index)),
                                                fromValueExpression
                                        )
                                )
                                .flatMap(statementStream -> statementStream);

        return Stream.concat(objectWithTypeRemoveStream, listValueInsertStatementStream);
    }

    protected Stream<Statement> arrayVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                     Expression parentIdValueExpression,
                                                                     GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                     GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                                     Expression fromValueExpression) {

        Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Insert> scalarOrEnumTypeVariableInsertStream = scalarOrEnumTypeVariableInsertStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                valueWithVariableContext,
                fromValueExpression
        );

        return Stream.concat(objectWithTypeRemoveStream, scalarOrEnumTypeVariableInsertStream);
    }

    protected Stream<Statement> listValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                 Expression parentIdValueExpression,
                                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArrayValueContext arrayValueContext,
                                                                 Expression fromValueExpression) {

        Stream<Statement> objectWithTypeRemoveStream = objectWithTypeRemoveStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Insert> listValueInsertStatementStream =
                arrayValueContext == null ?
                        Stream.empty() :
                        IntStream.range(0, arrayValueContext.value().size())
                                .mapToObj(index ->
                                        scalarOrEnumTypeFieldRelationInsertStream(
                                                parentFieldDefinitionContext,
                                                parentIdValueExpression,
                                                fieldDefinitionContext,
                                                dbValueUtil.valueToDBValue(arrayValueContext.value(index)),
                                                fromValueExpression
                                        )
                                )
                                .flatMap(statementStream -> statementStream);

        return Stream.concat(objectWithTypeRemoveStream, listValueInsertStatementStream);
    }

    protected Stream<Statement> listDefaultValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                        Expression parentIdValueExpression,
                                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                        GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext,
                                                                        Expression fromValueExpression) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                return listValueToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression, fieldDefinitionContext,
                        parentInputValueDefinitionContext.defaultValue().value().arrayValue(),
                        fromValueExpression
                );
            }
//            else {
//                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
//            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> objectTypeFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       Expression idValueExpression,
                                                                       Expression fromValueExpression,
                                                                       Expression toValueExpression) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();

        Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentFieldTypeName, fieldName);
        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentFieldTypeName, fieldName);

        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {
            Table parentTable = typeToTable(parentFieldDefinitionContext);
            Table table = typeToTable(fieldDefinitionContext);

            Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
            Column column = dbNameUtil.fieldToColumn(table, toFieldDefinition.get());
            Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column idColumn = dbNameUtil.fieldToColumn(table, idFieldDefinition.get());

            Expression parentColumnExpression;
            if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                parentColumnExpression = parentIdValueExpression;
            } else {
                parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
            }

            Expression columnExpression;
            if (column.getColumnName().equals(idColumn.getColumnName())) {
                columnExpression = idValueExpression;
            } else {
                columnExpression = selectFieldByIdExpression(table, column, idColumn, idValueExpression);
            }

            boolean mapWithType = mapper.mapWithType(parentFieldTypeName, fieldName);
            if (mapWithType) {
                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentFieldTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentFieldTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentFieldTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get());

                    if (fromValueExpression != null && toValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(fromValueExpression, toValueExpression))));
                    } else if (fromValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(fromValueExpression, columnExpression))));
                    } else if (toValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(parentColumnExpression, toValueExpression))));
                    } else {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(parentColumnExpression, columnExpression))));
                    }
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
                EqualsTo parentIdEqualsTo = new EqualsTo();
                parentIdEqualsTo.setLeftExpression(parentIdColumn);
                parentIdEqualsTo.setRightExpression(parentIdValueExpression);
                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(idColumn);
                idEqualsTo.setRightExpression(idValueExpression);

                if (mapper.anchor(parentFieldTypeName, fieldName)) {
                    if (toValueExpression != null) {
                        return Stream.of(updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(toValueExpression), parentIdEqualsTo));
                    } else {
                        return Stream.of(updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(columnExpression), parentIdEqualsTo));
                    }
                } else {
                    if (fromValueExpression != null) {
                        return Stream.of(updateExpression(table, Collections.singletonList(column), Collections.singletonList(fromValueExpression), idEqualsTo));
                    } else {
                        return Stream.of(updateExpression(table, Collections.singletonList(column), Collections.singletonList(parentColumnExpression), idEqualsTo));
                    }
                }
            }
        } else {
            GraphQLErrors graphQLErrors = new GraphQLErrors();
            if (parentIdFieldDefinition.isEmpty()) {
                graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentFieldTypeName));
            }
            if (idFieldDefinition.isEmpty()) {
                graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName));
            }
            if (fromFieldDefinition.isEmpty()) {
                graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            if (toFieldDefinition.isEmpty()) {
                graphQLErrors.add(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            throw graphQLErrors;
        }
    }

    protected Stream<Insert> scalarOrEnumTypeFieldRelationInsertStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       Expression valueExpression,
                                                                       Expression fromValueExpression) {
        String parentTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

        if (mapWithType) {
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get());

                    Expression parentColumnExpression;
                    if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                        parentColumnExpression = parentIdValueExpression;
                    } else {
                        parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                    }

                    if (fromValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(fromValueExpression, valueExpression))));
                    } else {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(parentColumnExpression, valueExpression))));
                    }
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
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLErrors;
            }
        } else {
            throw new GraphQLErrors(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
        }
    }

    protected Stream<Insert> scalarOrEnumTypeVariableInsertStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                  Expression parentIdValueExpression,
                                                                  GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                  GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                                  Expression fromValueExpression) {
        String parentTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

        if (mapWithType) {
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get());

                    Expression parentColumnExpression;
                    if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                        parentColumnExpression = parentIdValueExpression;
                    } else {
                        parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                    }

                    if (fromValueExpression != null) {
                        return Stream.of(insertSelectExpression(withTable, Arrays.asList(withParentColumn, withColumn), selectVariablesFromJsonArray(fromValueExpression, fieldDefinitionContext, valueWithVariableContext), true));
                    } else {
                        return Stream.of(insertSelectExpression(withTable, Arrays.asList(withParentColumn, withColumn), selectVariablesFromJsonArray(parentColumnExpression, fieldDefinitionContext, valueWithVariableContext), true));
                    }
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
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLErrors;
            }
        } else {
            throw new GraphQLErrors(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
        }
    }

    protected Stream<Statement> objectWithTypeRemoveStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                           Expression parentIdValueExpression,
                                                           GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           Expression fromValueExpression) {

        String parentTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

        if (mapWithType) {
            String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Expression parentColumnExpression;
                if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                    parentColumnExpression = parentIdValueExpression;
                } else {
                    parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                }

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get());

                    EqualsTo withParentColumnEqualsTo = new EqualsTo();
                    withParentColumnEqualsTo.setLeftExpression(withParentColumn);
                    if (fromValueExpression != null) {
                        withParentColumnEqualsTo.setRightExpression(fromValueExpression);
                    } else {
                        withParentColumnEqualsTo.setRightExpression(parentColumnExpression);
                    }
                    return Stream.of(removeExpression(withTable, withParentColumnEqualsTo));
                } else {
                    GraphQLErrors graphQLErrors = new GraphQLErrors();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLErrors;
                }
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLErrors;
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> objectWithTypeRemoveStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                           Expression parentIdValueExpression,
                                                           GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           Expression fromValueExpression,
                                                           List<GraphqlParser.ValueWithVariableContext> removeIDValueWithVariableList) {

        if (removeIDValueWithVariableList == null || removeIDValueWithVariableList.isEmpty()) {
            return Stream.empty();
        }

        String parentTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

        if (mapWithType) {
            String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);
            Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentFieldTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table parentTable = typeToTable(parentFieldDefinitionContext);
                    Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                    Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                    Table table = typeToTable(fieldDefinitionContext);
                    Column column = dbNameUtil.fieldToColumn(table, toFieldDefinition.get());
                    Column idColumn = dbNameUtil.fieldToColumn(table, idFieldDefinition.get());

                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get());

                    Expression parentColumnExpression;
                    if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                        parentColumnExpression = parentIdValueExpression;
                    } else {
                        parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                    }

                    EqualsTo withParentColumnEqualsTo = new EqualsTo();
                    withParentColumnEqualsTo.withLeftExpression(withParentColumn);
                    if (fromValueExpression != null) {
                        withParentColumnEqualsTo.setRightExpression(fromValueExpression);
                    } else {
                        withParentColumnEqualsTo.setRightExpression(parentColumnExpression);
                    }

                    InExpression inExpression = new InExpression().withLeftExpression(withColumn);
                    if (column.getColumnName().equals(idColumn.getColumnName())) {
                        inExpression.setRightItemsList(
                                new ExpressionList(
                                        removeIDValueWithVariableList.stream()
                                                .map(dbValueUtil::scalarValueWithVariableToDBValue)
                                                .collect(Collectors.toList())
                                )
                        );
                    } else {
                        inExpression.setRightExpression(selectFieldByIdExpressionList(table, column, idColumn, removeIDValueWithVariableList.stream().map(dbValueUtil::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
                    }
                    return Stream.of(removeExpression(withTable, new MultiAndExpression(Arrays.asList(withParentColumnEqualsTo, inExpression))));

                } else {
                    GraphQLErrors graphQLErrors = new GraphQLErrors();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLErrors;
                }
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLErrors;
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> objectTypeFieldRelationRemoveStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                    Expression parentIdValueExpression,
                                                                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                    List<Expression> idValueExpressionList,
                                                                    Expression fromValueExpression) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentFieldTypeName, fieldName);

        if (!mapWithType) {
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentFieldTypeName, fieldName);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentFieldTypeName, fieldName);
            boolean anchor = mapper.anchor(parentFieldTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Table table = typeToTable(fieldDefinitionContext);
                Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());
                Column column = dbNameUtil.fieldToColumn(table, toFieldDefinition.get());
                Column idColumn = dbNameUtil.fieldToColumn(table, idFieldDefinition.get());

                Expression parentColumnExpression;
                if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                    parentColumnExpression = parentIdValueExpression;
                } else {
                    parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                }

                EqualsTo parentColumnEqualsTo = new EqualsTo();
                parentColumnEqualsTo.setLeftExpression(column);
                if (fromValueExpression != null) {
                    parentColumnEqualsTo.setRightExpression(fromValueExpression);
                } else {
                    parentColumnEqualsTo.setRightExpression(parentColumnExpression);
                }

                if (idValueExpressionList != null && idValueExpressionList.size() > 0) {
                    InExpression idColumnNotIn = new InExpression();
                    idColumnNotIn.setLeftExpression(idColumn);
                    idColumnNotIn.setNot(true);
                    idColumnNotIn.setRightItemsList(new ExpressionList(idValueExpressionList));

                    if (anchor) {
                        return Stream.of(
                                updateExpression(parentTable, new UpdateSet(parentColumn, new NullValue()), parentIdColumn, parentIdValueExpression)
//                                , removeExpression(table, new MultiAndExpression(Arrays.asList(parentColumnEqualsTo, idColumnNotIn)))
                        );
                    } else {
                        return Stream.of(
                                updateExpression(table, new UpdateSet(column, new NullValue()), new MultiAndExpression(Arrays.asList(parentColumnEqualsTo, idColumnNotIn)))
//                                , removeExpression(table, new MultiAndExpression(Arrays.asList(parentColumnEqualsTo, idColumnNotIn)))
                        );
                    }
                } else {
                    if (anchor) {
                        return Stream.of(
                                updateExpression(parentTable, new UpdateSet(parentColumn, new NullValue()), parentIdColumn, parentIdValueExpression)
//                                , removeExpression(table, parentColumnEqualsTo)
                        );
                    } else {
                        return Stream.of(
                                updateExpression(table, new UpdateSet(column, new NullValue()), parentColumnEqualsTo)
//                                , removeExpression(table, parentColumnEqualsTo)
                        );
                    }
                }
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentFieldTypeName));
                }
                if (idFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName));
                }
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (toFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLErrors;
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> objectTypeFieldRelationRemoveStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                    Expression parentIdValueExpression,
                                                                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                    Expression fromValueExpression,
                                                                    List<GraphqlParser.ValueWithVariableContext> removeIDValueWithVariableList) {

        if (removeIDValueWithVariableList == null || removeIDValueWithVariableList.isEmpty()) {
            return Stream.empty();
        }

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentFieldTypeName, fieldName);

        if (!mapWithType) {
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentFieldTypeName, fieldName);
            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentFieldTypeName, fieldName);

            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Table table = typeToTable(fieldDefinitionContext);
                Column parentColumn = dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = dbNameUtil.fieldToColumn(parentTable, parentIdFieldDefinition.get());
                Column column = dbNameUtil.fieldToColumn(table, toFieldDefinition.get());
                Column idColumn = dbNameUtil.fieldToColumn(table, idFieldDefinition.get());

                Expression parentColumnExpression;
                if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                    parentColumnExpression = parentIdValueExpression;
                } else {
                    parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                }

                EqualsTo parentColumnEqualsTo = new EqualsTo();
                parentColumnEqualsTo.setLeftExpression(column);
                if (fromValueExpression != null) {
                    parentColumnEqualsTo.setRightExpression(fromValueExpression);
                } else {
                    parentColumnEqualsTo.setRightExpression(parentColumnExpression);
                }
                InExpression inIDExpression = new InExpression()
                        .withLeftExpression(idColumn)
                        .withRightItemsList(new ExpressionList(removeIDValueWithVariableList.stream().map(dbValueUtil::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
                return Stream.of(updateExpression(table, new UpdateSet(column, new NullValue()), new MultiAndExpression(Arrays.asList(parentColumnEqualsTo, inIDExpression))));
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(parentFieldTypeName));
                }
                if (idFieldDefinition.isEmpty()) {
                    graphQLErrors.add(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName));
                }
                if (fromFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (toFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLErrors;
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext,
                                                                 Hammurabi.MutationType mutationType) {

        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .collect(Collectors.toList());

        Optional<Expression> whereExpression = graphQLArgumentsToWhere.objectValueWithVariableToWhereExpression(fieldDefinitionContext, argumentsContext);

        Optional<Statement> statement;
        switch (mutationType) {
            case UPDATE:
                statement = argumentsToUpdate(dbNameUtil.typeToTable(manager.getFieldTypeName(fieldDefinitionContext.type()), 1), fieldDefinitionContext.type(), fieldList, argumentsContext, whereExpression.orElse(null));
                break;
            case DELETE:
                statement = Optional.of(argumentsToDelete(dbNameUtil.typeToTable(manager.getFieldTypeName(fieldDefinitionContext.type()), 1), fieldDefinitionContext.type(), argumentsContext, whereExpression.orElse(null)));
                break;
            default:
                statement = argumentsToInsert(table, fieldDefinitionContext.type(), fieldList, argumentsContext);
        }
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if ((idArgumentContext.isEmpty() || idArgumentContext.get().valueWithVariable().NullValue() != null) && idFieldName.isPresent()) {
            return Stream.concat(statement.stream(), Stream.of(dbValueUtil.createInsertIdSetStatement(typeName, idFieldName.get(), 0, 0)));
        }
        return statement.stream();
    }

    protected Stream<Statement> objectValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               int level,
                                                                               int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .collect(Collectors.toList());

        Insert insert = objectValueWithVariableToInsert(table, fieldDefinitionContext.type(), fieldList, objectValueWithVariableContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if ((idObjectFieldWithVariable.isEmpty() || idObjectFieldWithVariable.get().valueWithVariable().NullValue() != null) && idFieldName.isPresent()) {
            return Stream.of(insert, dbValueUtil.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
        }
        return Stream.of(insert);
    }

    protected Stream<Statement> objectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext,
                                                                   int level,
                                                                   int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .collect(Collectors.toList());

        Insert insert = objectValueToInsert(table, fieldDefinitionContext.type(), fieldList, objectValueContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);
        if ((idObjectField.isEmpty() || idObjectField.get().value().NullValue() != null) && idFieldName.isPresent()) {
            return Stream.of(insert, dbValueUtil.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
        }
        return Stream.of(insert);
    }

    protected Optional<Statement> argumentsToInsert(Table table,
                                                    GraphqlParser.TypeContext typeContext,
                                                    List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                    GraphqlParser.ArgumentsContext argumentsContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .filter(manager::isNotFetchField)
                                .map(fieldDefinitionContext ->
                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                .map(argumentContext -> dbNameUtil.fieldToColumn(table, argumentContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .filter(manager::isNotFetchField)
                                        .map(fieldDefinitionContext ->
                                                manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                        .map(argumentContext -> argumentToDBValue(fieldDefinitionContext, argumentContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList())
        );
        if (columnList.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(insertExpression(table, columnList, expressionList, true));
    }

    protected Optional<Statement> argumentsToUpdate(Table table,
                                                    GraphqlParser.TypeContext typeContext,
                                                    List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                    GraphqlParser.ArgumentsContext argumentsContext,
                                                    Expression where) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (where == null) {
            GraphqlParser.ArgumentContext idArgument = manager.getIDArgument(typeContext, argumentsContext)
                    .orElseThrow(() -> new GraphQLErrors(ID_ARGUMENT_NOT_EXIST.bind(argumentsContext.getText())));
            Column idColumn = dbNameUtil.fieldToColumn(table, idArgument);
            Expression idValue = argumentToDBValue(
                    manager.getField(fieldTypeName, idArgument.name().getText())
                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(fieldTypeName, idArgument.name().getText()))),
                    idArgument
            );

            List<UpdateSet> updateSetList = inputValueDefinitionContextList.stream()
                    .flatMap(inputValueDefinitionContext ->
                            manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                    .filter(fieldDefinitionContext -> !manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                    .filter(manager::isNotFetchField)
                                    .flatMap(fieldDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext))
                                    .stream()
                    )
                    .filter(argumentContext -> !argumentContext.name().getText().equals(idArgument.name().getText()))
                    .filter(argumentContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                    .map(argumentContext ->
                            new UpdateSet(
                                    dbNameUtil.fieldToColumn(table, argumentContext),
                                    argumentToDBValue(
                                            manager.getField(fieldTypeName, argumentContext.name().getText())
                                                    .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(fieldTypeName, argumentContext.name().getText()))),
                                            argumentContext
                                    )
                            )
                    )
                    .collect(Collectors.toList());
            if (updateSetList.size() == 0) {
                return Optional.empty();
            }
            return Optional.of(updateExpression(table, updateSetList, idColumn, idValue));
        }

        List<UpdateSet> updateSetList = inputValueDefinitionContextList.stream()
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                .filter(fieldDefinitionContext -> !manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                .filter(manager::isNotFetchField)
                                .flatMap(fieldDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext))
                                .stream()
                )
                .filter(argumentContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(argumentContext.name().getText())))
                .map(argumentContext ->
                        new UpdateSet(
                                dbNameUtil.fieldToColumn(table, argumentContext),
                                argumentToDBValue(
                                        manager.getField(fieldTypeName, argumentContext.name().getText())
                                                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(fieldTypeName, argumentContext.name().getText()))),
                                        argumentContext
                                )
                        )
                )
                .collect(Collectors.toList());

        if (updateSetList.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(updateExpression(table, updateSetList, where));
    }

    protected Statement argumentsToDelete(Table table,
                                          GraphqlParser.TypeContext typeContext,
                                          GraphqlParser.ArgumentsContext argumentsContext,
                                          Expression where) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (where == null) {
            GraphqlParser.ArgumentContext idArgument = manager.getIDArgument(typeContext, argumentsContext)
                    .orElseThrow(() -> new GraphQLErrors(ID_ARGUMENT_NOT_EXIST.bind(argumentsContext.getText())));
            Column idColumn = dbNameUtil.fieldToColumn(table, idArgument);
            Expression idValue = argumentToDBValue(
                    manager.getField(fieldTypeName, idArgument.name().getText())
                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(fieldTypeName, idArgument.name().getText()))),
                    idArgument
            );
            return deleteExpression(table, idColumn, idValue);
        }
        return deleteExpression(table, where);
    }

    protected Insert objectValueWithVariableToInsert(Table table,
                                                     GraphqlParser.TypeContext typeContext,
                                                     List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                     GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .filter(manager::isNotFetchField)
                                .map(fieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext -> dbNameUtil.fieldToColumn(table, objectFieldWithVariableContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .filter(manager::isNotFetchField)
                                        .map(fieldDefinitionContext ->
                                                manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                        .map(objectFieldWithVariableContext -> objectFieldWithVariableToDBValue(fieldDefinitionContext, objectFieldWithVariableContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList, true);
    }

    protected Stream<Insert> objectVariableToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                    GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                    GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .collect(Collectors.toList());

        List<Column> columnList = fieldList.stream()
                .map(subFieldDefinitionContext -> dbNameUtil.fieldToColumn(table, subFieldDefinitionContext))
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                fieldList.stream()
                        .map(inputValueDefinitionContext -> dbValueUtil.objectFieldVariableToDBValue(inputValueDefinitionContext, valueWithVariableContext))
                        .collect(Collectors.toList())
        );
        return Stream.of(insertExpression(table, columnList, expressionList, true));
    }

    protected Stream<Insert> arrayObjectVariableToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                         GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                         GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .collect(Collectors.toList());

        List<Column> columnList = fieldList.stream()
                .map(subFieldDefinitionContext -> dbNameUtil.fieldToColumn(table, subFieldDefinitionContext))
                .collect(Collectors.toList());

        Select select = selectVariablesFromJsonObjectArray(fieldList, valueWithVariableContext);

        return Stream.of(insertSelectExpression(table, columnList, select, true));
    }

    protected Select selectVariablesFromJsonObjectArray(List<GraphqlParser.InputValueDefinitionContext> fieldList,
                                                        GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Select select = new Select();
        JsonTable jsonTable = new JsonTable();
        JdbcNamedParameter jdbcNamedParameter = new JdbcNamedParameter();
        jdbcNamedParameter.setName(valueWithVariableContext.variable().name().getText());
        jsonTable.setJson(jdbcNamedParameter);
        jsonTable.setPath(new StringValue("$[*]"));

        List<ColumnDefinition> columnDefinitions = fieldList.stream()
                .map(subFieldDefinitionContext -> {
                            ColumnDefinition columnDefinition = new ColumnDefinition();
                            columnDefinition.setColumnName(dbNameUtil.graphqlFieldNameToColumnName(subFieldDefinitionContext.name().getText()));
                            columnDefinition.setColDataType(buildJsonColumnDataType(subFieldDefinitionContext.type()));
                            columnDefinition.addColumnSpecs("PATH", "'$." + subFieldDefinitionContext.name().getText() + "'");
                            return columnDefinition;
                        }
                )
                .collect(Collectors.toList());

        jsonTable.setColumnDefinitions(columnDefinitions);
        jsonTable.setAlias(new Alias(valueWithVariableContext.variable().name().getText()));

        PlainSelect body = new PlainSelect();
        body.addSelectItems(new AllColumns());
        body.setFromItem(jsonTable);
        select.setSelectBody(body);
        return select;
    }

    protected ColDataType buildJsonColumnDataType(GraphqlParser.TypeContext typeContext) {
        ColDataType colDataType = new ColDataType();
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        if (manager.isEnum(fieldTypeName)) {
            colDataType.setDataType("INT");
        } else if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                    colDataType.setDataType("VARCHAR");
                    colDataType.setArgumentsStringList(Collections.singletonList("255"));
                    break;
                case "Boolean":
                    colDataType.setDataType("BOOL");
                    break;
                case "Int":
                    colDataType.setDataType("INT");
                    break;
                case "Float":
                    colDataType.setDataType("FLOAT");
                    break;
                case "BigInteger":
                    colDataType.setDataType("BIGINT");
                    break;
                case "BigDecimal":
                    colDataType.setDataType("DECIMAL");
                    break;
                case "Date":
                    colDataType.setDataType("DATE");
                    break;
                case "Time":
                    colDataType.setDataType("TIME");
                    break;
                case "DateTime":
                    colDataType.setDataType("DATETIME");
                    break;
                case "Timestamp":
                    colDataType.setDataType("TIMESTAMP");
                    break;
            }
        } else {
            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
        }
        return colDataType;
    }

    protected Select selectVariablesFromJsonArray(Expression parentColumnExpression,
                                                  GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                  GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Select select = new Select();
        JsonTable jsonTable = new JsonTable();
        JdbcNamedParameter jdbcNamedParameter = new JdbcNamedParameter();
        jdbcNamedParameter.setName(valueWithVariableContext.variable().name().getText());
        jsonTable.setJson(jdbcNamedParameter);
        jsonTable.setPath(new StringValue("$[*]"));
        String columnName = dbNameUtil.graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText());
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(columnName);
        columnDefinition.setColDataType(buildJsonColumnDataType(fieldDefinitionContext.type()));
        columnDefinition.addColumnSpecs("PATH", "'$'");
        jsonTable.setColumnDefinitions(Collections.singletonList(columnDefinition));
        jsonTable.setAlias(new Alias(valueWithVariableContext.variable().name().getText()));

        PlainSelect body = new PlainSelect();
        body.addSelectItems(new SelectExpressionItem(parentColumnExpression), new SelectExpressionItem(new Column(columnName)));
        body.setFromItem(jsonTable);
        select.setSelectBody(body);
        return select;
    }

    protected Insert objectValueToInsert(Table table,
                                         GraphqlParser.TypeContext typeContext,
                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                         GraphqlParser.ObjectValueContext objectValueContext) {


        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .filter(manager::isNotFetchField)
                                .map(fieldDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                .map(objectFieldContext -> dbNameUtil.fieldToColumn(table, objectFieldContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .filter(manager::isNotFetchField)
                                        .map(fieldDefinitionContext ->
                                                manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                        .map(objectFieldContext -> objectFieldToDBValue(fieldDefinitionContext, objectFieldContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList, true);
    }

    protected Expression argumentToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.scalarValueWithVariableToDBValue(argumentContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.enumValueWithVariableToDBValue(argumentContext.valueWithVariable());
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(argumentContext.getText()));
    }

    protected Expression objectFieldWithVariableToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.enumValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(objectFieldWithVariableContext.getText()));
    }

    protected Expression objectFieldToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.scalarValueToDBValue(objectFieldContext.value());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return dbValueUtil.enumValueToDBValue(objectFieldContext.value());
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(objectFieldContext.getText()));
    }

    protected Expression defaultValueToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return dbValueUtil.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return dbValueUtil.enumValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(inputValueDefinitionContext.getText()));
                }
            }
//            else {
//                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
//            }
        }
        return null;
    }

    protected Table typeToTable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return typeToTable(fieldDefinitionContext.type());
    }

    protected Table typeToTable(GraphqlParser.TypeContext typeContext) {
        return dbNameUtil.typeToTable(manager.getFieldTypeName(typeContext));
    }

    protected Column defaultToColumn(Table table, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                return dbNameUtil.fieldToColumn(table, fieldDefinitionContext.get());
            } else {
                throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        }
        return null;
    }

    public UserVariable createInsertIdUserVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level, int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        return idFieldName.map(fieldName -> dbValueUtil.createInsertIdUserVariable(typeName, fieldName, level, index)).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));
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
            List<Expression> values = columnList.stream()
                    .map(this::buildValuesFunction)
                    .collect(Collectors.toList());
            insert.setDuplicateUpdateExpressionList(values);
        }
        return insert;
    }

    protected Update updateExpression(Table table,
                                      List<UpdateSet> updateSetList,
                                      Column idColumn,
                                      Expression idValue) {
        Update update = new Update();
        update.setTable(table);
        updateSetList.forEach(update::addUpdateSet);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(idColumn);
        equalsTo.setRightExpression(idValue);
        update.setWhere(equalsTo);
        return update;
    }

    protected Update updateExpression(Table table,
                                      UpdateSet updateSet,
                                      Column idColumn,
                                      Expression idValue) {
        Update update = new Update();
        update.setTable(table);
        update.addUpdateSet(updateSet);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(idColumn);
        equalsTo.setRightExpression(idValue);
        update.setWhere(equalsTo);
        return update;
    }

    protected Update updateExpression(Table table,
                                      List<UpdateSet> updateSetList,
                                      Expression where) {
        Update update = new Update();
        update.setTable(table);
        updateSetList.forEach(update::addUpdateSet);
        update.setWhere(where);
        return update;
    }

    protected Update updateExpression(Table table,
                                      UpdateSet updateSet,
                                      Expression where) {
        Update update = new Update();
        update.setTable(table);
        update.addUpdateSet(updateSet);
        update.setWhere(where);
        return update;
    }

    protected Delete deleteExpression(Table table,
                                      Column idColumn,
                                      Expression idValue) {
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(idColumn);
        equalsTo.setRightExpression(idValue);
        delete.setWhere(equalsTo);
        return delete;
    }

    protected Function buildValuesFunction(Column column) {
        Function function = new Function();
        function.setName("VALUES");
        function.setParameters(new ExpressionList(Collections.singletonList(column)));
        return function;
    }

    protected Insert insertSelectExpression(Table table,
                                            List<Column> columnList,
                                            Select select,
                                            boolean useDuplicate) {
        Insert insert = new Insert();
        insert.setTable(table);
        insert.setColumns(columnList);
        insert.setSelect(select);
        if (useDuplicate && columnList.size() > 0) {
            insert.setUseDuplicate(true);
            insert.setDuplicateUpdateColumns(columnList);
            List<Expression> values = columnList.stream()
                    .map(column -> {
                                Function function = new Function();
                                function.setName("VALUES");
                                function.setParameters(new ExpressionList(Collections.singletonList(column)));
                                return function;
                            }
                    )
                    .collect(Collectors.toList());
            insert.setDuplicateUpdateExpressionList(values);
        }
        return insert;
    }

    protected Update updateExpression(Table table,
                                      List<Column> columnList,
                                      List<Expression> expressionList,
                                      Expression where) {
        Update update = new Update();
        update.setTable(table);
        UpdateSet updateSet = new UpdateSet();
        updateSet.setColumns(new ArrayList<>(columnList));
        updateSet.setExpressions(new ArrayList<>(expressionList));
        update.addUpdateSet(updateSet);
        update.setWhere(where);
        return update;
    }

    protected Update removeExpression(Table table, Expression where) {
        Update update = new Update();
        update.setTable(table);
        Column deprecatedField = dbNameUtil.fieldToColumn(table, DEPRECATED_FIELD_NAME);
        UpdateSet updateSet = new UpdateSet();
        updateSet.add(deprecatedField, new LongValue(1));
        update.addUpdateSet(updateSet);
        update.setWhere(where);
        return update;
    }

    protected Delete deleteExpression(Table table, Expression where) {
        Delete delete = new Delete();
        delete.setTable(table);
        delete.setWhere(where);
        return delete;
    }

    protected SubSelect selectFieldByIdExpression(Table table,
                                                  Column selectColumn,
                                                  Column idColumn,
                                                  Expression idFieldValueExpression) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        subBody.setFromItem(table);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(selectColumn);
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(idColumn);
        subEqualsTo.setRightExpression(idFieldValueExpression);
        subBody.setWhere(subEqualsTo);
        subSelect.setSelectBody(subBody);
        return subSelect;
    }

    protected SubSelect selectFieldByIdExpressionList(Table table,
                                                      Column selectColumn,
                                                      Column idColumn,
                                                      List<Expression> idFieldValueExpressionList) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        subBody.setFromItem(table);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(selectColumn);
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(idColumn);
        inExpression.setRightItemsList(new ExpressionList(idFieldValueExpressionList));
        subBody.setWhere(inExpression);
        subSelect.setSelectBody(subBody);
        return subSelect;
    }
}
