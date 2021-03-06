package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.common.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;
import static io.graphoenix.spi.error.GraphQLErrorType.*;
import static io.graphoenix.spi.error.GraphQLErrorType.MAP_TO_FIELD_NOT_EXIST;

public class GraphQLMutationToStatements {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;
    private final GraphQLQueryToSelect graphqlQueryToSelect;

    public GraphQLMutationToStatements(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, GraphQLQueryToSelect graphqlQueryToSelect) {
        this.manager = manager;
        this.mapper = mapper;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
    }

    public Stream<String> createStatementsSQL(String graphql) {
        return operationDefinitionToStatementStream(DOCUMENT_UTIL.graphqlToOperation(graphql)).map(Object::toString);
    }

    public Stream<Statement> createStatements(String graphql) {
        return operationDefinitionToStatementStream(DOCUMENT_UTIL.graphqlToOperation(graphql));
    }

    protected Stream<Statement> operationDefinitionToStatementStream(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() != null && operationDefinitionContext.operationType().MUTATION() != null) {
            Optional<GraphqlParser.OperationTypeDefinitionContext> mutationOperationTypeDefinition = manager.getMutationOperationTypeDefinition();
            if (mutationOperationTypeDefinition.isPresent()) {
                return Stream.concat(
                        operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream),
                        Stream.of(graphqlQueryToSelect.objectSelectionToSelect(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()))
                );
            }
        }
        throw new GraphQLProblem(MUTATION_NOT_EXIST);
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getObjectFieldDefinition(mutationTypeName, selectionContext.field().name().getText()));

        if (mutationFieldTypeDefinitionContext.isPresent()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = mutationFieldTypeDefinitionContext.get();
            GraphqlParser.ArgumentsContext argumentsContext = selectionContext.field().arguments();
            return argumentsToStatementStream(fieldDefinitionContext, argumentsContext);
        }
        throw new GraphQLProblem(MUTATION_NOT_EXIST);
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext) {

        Stream<Statement> insertStatementStream = argumentsToInsertStatementStream(fieldDefinitionContext, argumentsContext);

        Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext).map(DB_VALUE_UTIL::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, 0, 0));

        Stream<Statement> objectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .map(argumentContext ->
                                                                        objectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                argumentContext.valueWithVariable().objectValueWithVariable(),
                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                mapper.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable())
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0,
                                                                                0
                                                                        )
                                                                ).orElseGet(() ->
                                                                objectDefaultValueToStatementStream(
                                                                        fieldDefinitionContext,
                                                                        idValueExpression,
                                                                        subFieldDefinitionContext,
                                                                        inputObjectTypeDefinitionContext,
                                                                        inputValueDefinitionContext,
                                                                        mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                        0,
                                                                        0
                                                                )
                                                        )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .filter(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable() != null)
                                                                .map(argumentContext ->
                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0
                                                                        )
                                                                )
                                                                .orElseGet(() ->
                                                                        listObjectDefaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                0
                                                                        )
                                                                )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );


        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                .filter(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable() != null)
                                                .map(argumentContext ->
                                                        listValueWithVariableToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
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

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);

        Expression idValueExpression = objectIdFieldWithVariableContext.map(DB_VALUE_UTIL::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> insertStatementStream = objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueWithVariableContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext ->
                                                                        objectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                mapper.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable())
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
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
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );


        Stream<Statement> updateMapObjectFieldStatementStream = mapObjectTypeFieldRelationStatementStream(
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
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable() != null)
                                                                .map(objectFieldWithVariableContext ->
                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
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
                                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                                                                                level + 1
                                                                        )
                                                                )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );


        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable() != null)
                                                .map(objectFieldWithVariableContext ->
                                                        listValueWithVariableToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null)
                                                        )
                                                )
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );

        return Stream.concat(insertStatementStream, Stream.concat(updateMapObjectFieldStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
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

        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);

        Expression idValueExpression = objectIdFieldContext.map(DB_VALUE_UTIL::createIdValueExpression).orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index));
        Stream<Statement> insertStatementStream = objectValueToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
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
                                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
                                                                                mapper.getMapToValueFromObjectField(subFieldDefinitionContext, objectFieldContext.value().objectValue())
                                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
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
                                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );

        Stream<Statement> updateMapObjectFieldStatementStream = mapObjectTypeFieldRelationStatementStream(
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
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                                .filter(objectFieldContext -> objectFieldContext.value().arrayValue() != null)
                                                                .map(objectFieldContext ->
                                                                        listObjectValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldContext.value().arrayValue(),
                                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
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
                                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
                                                                                level + 1
                                                                        )
                                                                )
                                                ).orElseThrow(() -> new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type()))))
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );

        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .flatMap(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                .filter(objectFieldContext -> objectFieldContext.value().arrayValue() != null)
                                                .map(objectFieldContext ->
                                                        listValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                objectFieldContext.value().arrayValue(),
                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null)
                                                        )
                                                )
                                                .orElseGet(() ->
                                                        listDefaultValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                mapper.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)
                                                                        .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null)
                                                        )
                                                )
                                ).orElseThrow(() -> new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), inputValueDefinitionContext.name().getText())))
                );

        return Stream.concat(insertStatementStream, Stream.concat(updateMapObjectFieldStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
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
                                .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
                        level,
                        index
                );
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
            }
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

        Stream<Statement> deleteWithTypeStatementStream = deleteWithTypeStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listObjectValueInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> objectValueWithVariableToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable(),
                        fromValueExpression,
                        mapper.getMapToValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())
                                .map(DB_VALUE_UTIL::scalarValueWithVariableToDBValue).orElse(null),
                        level,
                        index
                        )
                )
                .flatMap(statementStream -> statementStream);

        List<Expression> idValueExpressionList = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index ->
                        manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())
                                .map(DB_VALUE_UTIL::createIdValueExpression)
                                .orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index)))
                .collect(Collectors.toList());

        Stream<Statement> deleteObjectTypeFieldRelationStatementStream = deleteObjectTypeFieldRelationStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpressionList,
                fromValueExpression
        );

        return Stream.concat(deleteWithTypeStatementStream, Stream.concat(listObjectValueInsertStatementStream, deleteObjectTypeFieldRelationStatementStream));
    }

    protected Stream<Statement> listObjectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                       GraphqlParser.ArrayValueContext arrayValueContext,
                                                                       Expression fromValueExpression,
                                                                       int level) {

        Stream<Statement> deleteWithTypeStatementStream = deleteWithTypeStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listObjectValueInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> objectValueToStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        arrayValueContext.value(index).objectValue(),
                        fromValueExpression,
                        mapper.getMapToValueFromObjectField(fieldDefinitionContext, arrayValueContext.value(index).objectValue())
                                .map(DB_VALUE_UTIL::scalarValueToDBValue).orElse(null),
                        level,
                        index
                        )
                )
                .flatMap(statementStream -> statementStream);

        List<Expression> idValueExpressionList = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index ->
                        manager.getIDObjectField(fieldDefinitionContext.type(), arrayValueContext.value(index).objectValue())
                                .map(DB_VALUE_UTIL::createIdValueExpression)
                                .orElseGet(() -> createInsertIdUserVariable(fieldDefinitionContext, level, index)))
                .collect(Collectors.toList());

        Stream<Statement> deleteObjectTypeFieldRelationStatementStream = deleteObjectTypeFieldRelationStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpressionList,
                fromValueExpression
        );

        return Stream.concat(deleteWithTypeStatementStream, Stream.concat(listObjectValueInsertStatementStream, deleteObjectTypeFieldRelationStatementStream));
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
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                             Expression parentIdValueExpression,
                                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                             Expression fromValueExpression) {

        Stream<Statement> listValueDeleteStatementStream = deleteWithTypeStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> mapScalarOrEnumTypeFieldRelationStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        DB_VALUE_UTIL.valueWithVariableToDBValue(arrayValueWithVariableContext.valueWithVariable(index)),
                        fromValueExpression
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listValueDeleteStatementStream, listValueInsertStatementStream);
    }


    protected Stream<Statement> listValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                 Expression parentIdValueExpression,
                                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArrayValueContext arrayValueContext,
                                                                 Expression fromValueExpression) {

        Stream<Statement> listValueDeleteStatementStream = deleteWithTypeStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> mapScalarOrEnumTypeFieldRelationStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        DB_VALUE_UTIL.valueToDBValue(arrayValueContext.value(index)),
                        fromValueExpression
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listValueDeleteStatementStream, listValueInsertStatementStream);
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
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(parentInputValueDefinitionContext.getText()));
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> mapObjectTypeFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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

            Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
            Column column = DB_NAME_UTIL.fieldToColumn(table, toFieldDefinition.get());
            Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column idColumn = DB_NAME_UTIL.fieldToColumn(table, idFieldDefinition.get());

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
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithToFieldDefinition.get());

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
                EqualsTo parentIdEqualsTo = new EqualsTo();
                parentIdEqualsTo.setLeftExpression(parentIdColumn);
                parentIdEqualsTo.setRightExpression(parentIdValueExpression);
                EqualsTo idEqualsTo = new EqualsTo();
                idEqualsTo.setLeftExpression(idColumn);
                idEqualsTo.setRightExpression(idValueExpression);

                if (fromValueExpression != null && toValueExpression == null) {
                    return Stream.of(updateExpression(table, Collections.singletonList(column), Collections.singletonList(fromValueExpression), idEqualsTo));
                } else if (fromValueExpression == null && toValueExpression != null) {
                    return Stream.of(updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(toValueExpression), parentIdEqualsTo));
                } else {
                    IsNullExpression parentColumnIsNull = new IsNullExpression();
                    parentColumnIsNull.setLeftExpression(parentColumn);

                    IsNullExpression columnIsNull = new IsNullExpression();
                    columnIsNull.setLeftExpression(column);

                    return Stream.of(
                            updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(columnExpression), new MultiAndExpression(Arrays.asList(parentColumnIsNull, parentIdEqualsTo))),
                            updateExpression(table, Collections.singletonList(column), Collections.singletonList(parentColumnExpression), new MultiAndExpression(Arrays.asList(columnIsNull, idEqualsTo)))
                    );
                }
            }
        } else {
            GraphQLProblem graphQLProblem = new GraphQLProblem();
            if (parentIdFieldDefinition.isEmpty()) {
                graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(parentFieldTypeName));
            }
            if (idFieldDefinition.isEmpty()) {
                graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName));
            }
            if (fromFieldDefinition.isEmpty()) {
                graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            if (toFieldDefinition.isEmpty()) {
                graphQLProblem.push(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            throw graphQLProblem;
        }
    }

    protected Stream<Statement> mapScalarOrEnumTypeFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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
                Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithFromFieldDefinition.get());
                    Column withColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithToFieldDefinition.get());

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
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLProblem;
            }
        } else {
            throw new GraphQLProblem(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
        }
    }

    protected Stream<Statement> deleteWithTypeStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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
                Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Expression parentColumnExpression;
                if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                    parentColumnExpression = parentIdValueExpression;
                } else {
                    parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                }

                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent()) {
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithObjectDefinition.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithFromFieldDefinition.get());

                    EqualsTo withParentColumnEqualsTo = new EqualsTo();
                    withParentColumnEqualsTo.setLeftExpression(withParentColumn);
                    if (fromValueExpression != null) {
                        withParentColumnEqualsTo.setRightExpression(fromValueExpression);
                    } else {
                        withParentColumnEqualsTo.setRightExpression(parentColumnExpression);
                    }
                    return Stream.of(deleteExpression(withTable, withParentColumnEqualsTo));
                } else {
                    GraphQLProblem graphQLProblem = new GraphQLProblem();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLProblem.push(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLProblem;
                }
            } else {
                GraphQLProblem graphQLProblem = new GraphQLProblem();
                if (fromFieldDefinition.isEmpty()) {
                    graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (parentIdFieldDefinition.isEmpty()) {
                    graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(parentTypeName));
                }
                throw graphQLProblem;
            }
        }
        return Stream.empty();
    }


    protected Stream<Statement> deleteObjectTypeFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                             Expression parentIdValueExpression,
                                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             List<Expression> idValueExpressionList,
                                                                             Expression fromValueExpression) {

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
            Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
            Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column column = DB_NAME_UTIL.fieldToColumn(table, toFieldDefinition.get());
            Column idColumn = DB_NAME_UTIL.fieldToColumn(table, idFieldDefinition.get());

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
                return Stream.of(deleteExpression(table, new MultiAndExpression(Arrays.asList(parentColumnEqualsTo, idColumnNotIn))));
            } else {
                return Stream.of(deleteExpression(table, parentColumnEqualsTo));
            }
        } else {
            GraphQLProblem graphQLProblem = new GraphQLProblem();
            if (parentIdFieldDefinition.isEmpty()) {
                graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(parentFieldTypeName));
            }
            if (idFieldDefinition.isEmpty()) {
                graphQLProblem.push(TYPE_ID_FIELD_NOT_EXIST.bind(fieldTypeName));
            }
            if (fromFieldDefinition.isEmpty()) {
                graphQLProblem.push(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            if (toFieldDefinition.isEmpty()) {
                graphQLProblem.push(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            throw graphQLProblem;
        }
    }

    protected Stream<Statement> argumentsToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext) {

        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = argumentsToInsert(table, fieldDefinitionContext.type(), fieldList, argumentsContext);

        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), 0, 0));
        }
        return Stream.of(insert);
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
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = objectValueWithVariableToInsert(table, fieldDefinitionContext.type(), fieldList, objectValueWithVariableContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariable.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
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
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = objectValueToInsert(table, fieldDefinitionContext.type(), fieldList, objectValueContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);
        if (idObjectField.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
        }

        return Stream.of(insert);
    }

    protected Insert argumentsToInsert(Table table,
                                       GraphqlParser.TypeContext typeContext,
                                       List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                       GraphqlParser.ArgumentsContext argumentsContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .map(fieldDefinitionContext ->
                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                .map(argumentContext -> DB_NAME_UTIL.fieldToColumn(table, argumentContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .map(fieldDefinitionContext ->
                                                manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                        .map(argumentContext -> argumentToDBValue(fieldDefinitionContext, argumentContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList, true);
    }


    protected Insert objectValueWithVariableToInsert(Table table,
                                                     GraphqlParser.TypeContext typeContext,
                                                     List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                     GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .map(fieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext -> DB_NAME_UTIL.fieldToColumn(table, objectFieldWithVariableContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .map(fieldDefinitionContext ->
                                                manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                        .map(objectFieldWithVariableContext -> objectFieldWithVariableToDBValue(fieldDefinitionContext, objectFieldWithVariableContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList, true);
    }


    protected Insert objectValueToInsert(Table table,
                                         GraphqlParser.TypeContext typeContext,
                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                         GraphqlParser.ObjectValueContext objectValueContext) {


        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                .map(fieldDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                .map(objectFieldContext -> DB_NAME_UTIL.fieldToColumn(table, objectFieldContext))
                                                .orElseGet(() -> defaultToColumn(table, typeContext, inputValueDefinitionContext))
                                )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext)
                                        .map(fieldDefinitionContext ->
                                                manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                        .map(objectFieldContext -> objectFieldToDBValue(fieldDefinitionContext, objectFieldContext))
                                                        .orElseGet(() -> defaultValueToDBValue(inputValueDefinitionContext))
                                        )
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList, true);
    }

    protected Expression argumentToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(argumentContext.valueWithVariable());
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(argumentContext.getText()));
    }

    protected Expression objectFieldWithVariableToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(objectFieldWithVariableContext.getText()));
    }

    protected Expression objectFieldToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.value());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueToDBValue(objectFieldContext.value());
        }
        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(objectFieldContext.getText()));
    }

    protected Expression defaultValueToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.enumValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else {
                    throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(inputValueDefinitionContext.getText()));
                }
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        }
        return null;
    }

    protected Table typeToTable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return typeToTable(fieldDefinitionContext.type());
    }

    protected Table typeToTable(GraphqlParser.TypeContext typeContext) {
        return DB_NAME_UTIL.typeToTable(manager.getFieldTypeName(typeContext));
    }

    protected Column defaultToColumn(Table table, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                return DB_NAME_UTIL.fieldToColumn(table, fieldDefinitionContext.get());
            } else {
                throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        }
        return null;
    }

    public UserVariable createInsertIdUserVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level, int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        return idFieldName.map(fieldName -> DB_VALUE_UTIL.createInsertIdUserVariable(typeName, fieldName, level, index)).orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));
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

    protected Update updateExpression(Table table,
                                      List<Column> columnList,
                                      List<Expression> expressionList,
                                      Expression where) {

        Update update = new Update();
        update.setTable(table);
        update.setColumns(columnList);
        update.setExpressions(expressionList);
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
}
