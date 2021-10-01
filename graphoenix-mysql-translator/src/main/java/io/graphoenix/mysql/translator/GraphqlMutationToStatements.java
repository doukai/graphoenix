package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.mysql.common.utils.DBNameUtil.DB_NAME_UTIL;
import static io.graphoenix.mysql.common.utils.DBValueUtil.DB_VALUE_UTIL;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrManager manager;
    private final GraphqlQueryToSelect graphqlQueryToSelect;

    public GraphqlMutationToStatements(GraphqlAntlrManager manager, GraphqlQueryToSelect graphqlQueryToSelect) {
        this.manager = manager;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
    }

    public List<String> createStatementsSql(String graphql) {
        return createSelectsSql(DocumentUtil.DOCUMENT_UTIL.graphqlToDocument(graphql));
    }

    public List<String> createSelectsSql(GraphqlParser.DocumentContext documentContext) {
        return createStatements(documentContext).getStatements().stream()
                .map(Statement::toString).collect(Collectors.toList());
    }

    public Statements createStatements(GraphqlParser.DocumentContext documentContext) {
        Statements statements = new Statements();
        statements.setStatements(documentContext.definition().stream().flatMap(this::createStatementStream).collect(Collectors.toList()));
        return statements;
    }

    protected Stream<Statement> createStatementStream(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() != null) {
            return operationDefinitionToStatementStream(definitionContext.operationDefinition());
        }
        return Stream.empty();
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
        return Stream.empty();
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText()));

        if (mutationFieldTypeDefinitionContext.isPresent()) {
            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = mutationFieldTypeDefinitionContext.get();
            GraphqlParser.ArgumentsContext argumentsContext = selectionContext.field().arguments();
            return argumentsToStatementStream(fieldDefinitionContext, argumentsContext);
        }

        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext) {

        Stream<Statement> insertStatementStream = argumentsToInsertStatementStream(fieldDefinitionContext, argumentsContext);

        Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext).map(DB_VALUE_UTIL::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, 0, 0));

        Stream<Statement> objectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
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
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)),
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable())),
                                                                                0,
                                                                                0
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)),
                                                                                0,
                                                                                0
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .map(argumentContext ->
                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)),
                                                                                0
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListObjectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext)),
                                                                                0
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);


        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                .map(argumentContext ->
                                                        listValueWithVariableToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                argumentContext.valueWithVariable().arrayValueWithVariable(),
                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext))
                                                        )
                                                )
                                                .orElse(
                                                        defaultListValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromArguments(fieldDefinitionContext, subFieldDefinitionContext, argumentsContext))
                                                        )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

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

        Expression idValueExpression = objectIdFieldWithVariableContext.map(DB_VALUE_UTIL::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> insertStatementStream = objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueWithVariableContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
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
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)),
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapToValueWithVariableFromObjectFieldWithVariable(subFieldDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable())),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);


        Stream<Statement> updateMapObjectFieldStatementStream = mapFieldRelationStatementStream(
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
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(subInputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext ->
                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)),
                                                                                level + 1
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListObjectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext)),
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);


        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                                .map(objectFieldWithVariableContext ->
                                                        listValueWithVariableToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext))
                                                        )
                                                )
                                                .orElse(
                                                        defaultListValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapFromValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, subFieldDefinitionContext, objectValueWithVariableContext))
                                                        )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

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

        Expression idValueExpression = objectIdFieldContext.map(DB_VALUE_UTIL::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index));
        Stream<Statement> insertStatementStream = objectValueToInsertStatementStream(fieldDefinitionContext, inputObjectTypeDefinitionContext, objectValueContext, level, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
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
                                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)),
                                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapToValueFromObjectField(subFieldDefinitionContext, objectFieldContext.value().objectValue())),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> updateMapObjectFieldStatementStream = mapFieldRelationStatementStream(
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
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
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
                                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)),
                                                                                level + 1
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListObjectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                subInputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext)),
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listInsertStatementStream = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .map(subFieldDefinitionContext ->
                                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                                .map(objectFieldContext ->
                                                        listValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                objectFieldContext.value().arrayValue(),
                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext))
                                                        )
                                                )
                                                .orElse(
                                                        defaultListValueToInsertStatementStream(
                                                                fieldDefinitionContext,
                                                                idValueExpression,
                                                                subFieldDefinitionContext,
                                                                inputValueDefinitionContext,
                                                                DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapFromValueFromObjectField(fieldDefinitionContext, subFieldDefinitionContext, objectValueContext))
                                                        )
                                                )
                                )
                )
                .filter(Optional::isPresent)
                .flatMap(Optional::get);

        return Stream.concat(insertStatementStream, Stream.concat(updateMapObjectFieldStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
    }

    protected Stream<Statement> defaultValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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
                        DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapToValueFromObjectField(fieldDefinitionContext, parentInputValueDefinitionContext.defaultValue().value().objectValue())),
                        level,
                        index
                );
            } else {
                //TODO
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

        Stream<Statement> listArgumentDeleteStatementStream = mapFieldRelationDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listArgumentInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> objectValueWithVariableToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable(),
                        fromValueExpression,
                        DB_VALUE_UTIL.scalarValueWithVariableToDBValue(manager.getMapToValueWithVariableFromObjectFieldWithVariable(fieldDefinitionContext, arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())),
                        level,
                        index
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listArgumentDeleteStatementStream, listArgumentInsertStatementStream);
    }

    protected Stream<Statement> listObjectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext,
                                                                       GraphqlParser.ArrayValueContext arrayValueContext,
                                                                       Expression fromValueExpression,
                                                                       int level) {

        Stream<Statement> listArgumentDeleteStatementStream = mapFieldRelationDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listArgumentInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> objectValueToStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinitionContext,
                        arrayValueContext.value(index).objectValue(),
                        fromValueExpression,
                        DB_VALUE_UTIL.scalarValueToDBValue(manager.getMapToValueFromObjectField(fieldDefinitionContext, arrayValueContext.value(index).objectValue())),
                        level,
                        index
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listArgumentDeleteStatementStream, listArgumentInsertStatementStream);
    }

    protected Stream<Statement> defaultListObjectValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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
                //TODO
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                             Expression parentIdValueExpression,
                                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                             GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                             Expression fromValueExpression) {

        Stream<Statement> listValueDeleteStatementStream = mapFieldRelationDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> mapFieldRelationStatementStream(
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

        Stream<Statement> listValueDeleteStatementStream = mapFieldRelationDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                fromValueExpression
        );

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> mapFieldRelationStatementStream(
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

    protected Stream<Statement> defaultListValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
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
                //TODO
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> mapFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                Expression parentIdValueExpression,
                                                                GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                Expression idValueExpression,
                                                                Expression fromValueExpression,
                                                                Expression toValueExpression) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);
        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);

        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);
            Table parentTable = typeToTable(parentFieldDefinitionContext);
            Table table = typeToTable(fieldDefinitionContext);
            Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
            Column column = DB_NAME_UTIL.fieldToColumn(table, toFieldDefinition.get());
            Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column idColumn = DB_NAME_UTIL.fieldToColumn(table, idFieldDefinition.get());
            EqualsTo parentIdEqualsTo = new EqualsTo();
            parentIdEqualsTo.setLeftExpression(parentIdColumn);
            parentIdEqualsTo.setRightExpression(parentIdValueExpression);
            EqualsTo idEqualsTo = new EqualsTo();
            idEqualsTo.setLeftExpression(idColumn);
            idEqualsTo.setRightExpression(idValueExpression);

            Expression parentColumnExpression;
            Expression columnExpression;

            if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                parentColumnExpression = parentIdValueExpression;
            } else {
                parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
            }
            if (column.getColumnName().equals(idColumn.getColumnName())) {
                columnExpression = idValueExpression;
            } else {
                columnExpression = selectFieldByIdExpression(table, column, idColumn, idValueExpression);
            }

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithTypeName.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithFromFieldName.get());
                    Column withColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithToFieldName.get());

                    if (fromValueExpression != null && toValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(fromValueExpression, toValueExpression))));
                    } else if (fromValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(fromValueExpression, columnExpression))));
                    } else if (toValueExpression != null) {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(parentColumnExpression, toValueExpression))));
                    } else {
                        return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(parentColumnExpression, columnExpression))));
                    }
                }
            } else {
                if (fromValueExpression != null && toValueExpression == null) {
                    return Stream.of(updateExpression(table, Collections.singletonList(column), Collections.singletonList(fromValueExpression), idEqualsTo));
                } else if (fromValueExpression == null && toValueExpression != null) {
                    return Stream.of(updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(toValueExpression), parentIdEqualsTo));
                } else if (fromValueExpression == null) {
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
        }

        return Stream.empty();
    }

    protected Stream<Statement> mapFieldRelationStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                Expression parentIdValueExpression,
                                                                GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                Expression valueExpression,
                                                                Expression fromValueExpression) {

        Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);

        if (mapWithTypeArgument.isPresent()) {
            String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
            Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);

            if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {
                Table parentTable = typeToTable(parentFieldDefinitionContext);
                Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
                Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());

                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithTypeName.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithFromFieldName.get());
                    Column withColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithToFieldName.get());

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
                }
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> mapFieldRelationDeleteStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                      Expression parentIdValueExpression,
                                                                      GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                      Expression fromValueExpression) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> idFieldDefinition = manager.getObjectTypeIDFieldDefinition(fieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);
        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(fieldDefinitionContext);

        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent() && idFieldDefinition.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);
            Table parentTable = typeToTable(parentFieldDefinitionContext);
            Column parentColumn = DB_NAME_UTIL.fieldToColumn(parentTable, fromFieldDefinition.get());
            Column parentIdColumn = DB_NAME_UTIL.fieldToColumn(parentTable, parentIdFieldDefinition.get());

            Expression parentColumnExpression;
            if (parentColumn.getColumnName().equals(parentIdColumn.getColumnName())) {
                parentColumnExpression = parentIdValueExpression;
            } else {
                parentColumnExpression = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
            }

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = DB_NAME_UTIL.typeToTable(mapWithTypeName.get());
                    Column withParentColumn = DB_NAME_UTIL.fieldToColumn(withTable, mapWithToFieldName.get());
                    EqualsTo withParentColumnEqualsTo = new EqualsTo();
                    withParentColumnEqualsTo.setLeftExpression(withParentColumn);

                    if (fromValueExpression != null) {
                        withParentColumnEqualsTo.setRightExpression(fromValueExpression);
                    } else {
                        withParentColumnEqualsTo.setRightExpression(parentColumnExpression);
                    }
                    return Stream.of(deleteExpression(withTable, withParentColumnEqualsTo));
                }
            } else {
                Table table = typeToTable(fieldDefinitionContext);
                Column column = DB_NAME_UTIL.fieldToColumn(table, toFieldDefinition.get());

                EqualsTo columnEqualsTo = new EqualsTo();
                columnEqualsTo.setLeftExpression(column);

                if (fromValueExpression != null) {
                    columnEqualsTo.setRightExpression(fromValueExpression);
                } else {
                    columnEqualsTo.setRightExpression(parentColumnExpression);
                }
                return Stream.of(deleteExpression(table, columnEqualsTo));
            }
        }
        return Stream.empty();
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
                                                .orElse(defaultToColumn(table, typeContext, inputValueDefinitionContext))
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
                                                        .orElse(defaultValueToDBValue(inputValueDefinitionContext))
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
                                                .orElse(defaultToColumn(table, typeContext, inputValueDefinitionContext))
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
                                                        .orElse(defaultValueToDBValue(inputValueDefinitionContext))
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
                                                .orElse(defaultToColumn(table, typeContext, inputValueDefinitionContext))
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
                                                        .orElse(defaultValueToDBValue(inputValueDefinitionContext))
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
        return null;
    }

    protected Expression objectFieldWithVariableToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        }
        return null;
    }

    protected Expression objectFieldToDBValue(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.value());
        } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueToDBValue(objectFieldContext.value());
        }
        return null;
    }

    protected Expression defaultValueToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    return DB_VALUE_UTIL.enumValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                }
            } else {
                //TODO
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
            }
        }
        return null;
    }

    public UserVariable createInsertIdUserVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level, int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        return idFieldName.map(fieldName -> DB_VALUE_UTIL.createInsertIdUserVariable(typeName, fieldName, level, index)).orElse(null);
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
