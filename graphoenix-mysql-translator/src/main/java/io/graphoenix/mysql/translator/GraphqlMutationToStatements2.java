package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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

public class GraphqlMutationToStatements2 {

    private final GraphqlAntlrManager manager;
    private final GraphqlQueryToSelect graphqlQueryToSelect;

    public GraphqlMutationToStatements2(GraphqlAntlrManager manager, GraphqlQueryToSelect graphqlQueryToSelect) {
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
                Select select = new Select();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
//                selectExpressionItem.setExpression(graphqlQueryToSelect.objectFieldSelectionToJsonObjectFunction(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()));
                body.setSelectItems(Collections.singletonList(selectExpressionItem));
                Table table = new Table("dual");
                body.setFromItem(table);
                select.setSelectBody(body);
                return Stream.concat(operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream), Stream.of(select));
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

        Expression idValueExpression = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext).map(this::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, 0, 0));

        Stream<Statement> objectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
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
                                                                                0,
                                                                                0
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
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
                                                                                0
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);


        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                                                .map(argumentContext ->
                                                                        listValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                argumentContext.valueWithVariable().arrayValueWithVariable()
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputValueDefinitionContext
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        return Stream.concat(insertStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream)));
    }

    protected Stream<Statement> objectValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                               Expression parentIdValueExpression,
                                                                               GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariable,
                                                                               int level,
                                                                               int index) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariable);

        Expression idValueExpression = objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> updateMapObjectFieldStatementStream = mapObjectFieldUpdateStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpression
        );

        Stream<Statement> insertStatementStream = objectValueWithVariableToInsertStatementStream(fieldDefinitionContext, objectValueWithVariable, level + 1, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariable, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext ->
                                                                        objectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariable, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext ->
                                                                        listObjectValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable(),
                                                                                level + 1
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListObjectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);


        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariable, inputValueDefinitionContext)
                                                                .map(objectFieldWithVariableContext ->
                                                                        listValueWithVariableToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                objectFieldWithVariableContext.valueWithVariable().arrayValueWithVariable()
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputValueDefinitionContext
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        return Stream.concat(updateMapObjectFieldStatementStream, Stream.concat(insertStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
    }

    protected Stream<Statement> objectValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                             Expression parentIdValueExpression,
                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                             GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                             GraphqlParser.ObjectValueContext objectValue,
                                                             int level,
                                                             int index) {

        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(fieldDefinitionContext.type(), objectValue);

        Expression idValueExpression = objectIdFieldContext.map(this::createIdValueExpression).orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index));

        Stream<Statement> updateMapObjectFieldStatementStream = mapObjectFieldUpdateStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                idValueExpression
        );

        Stream<Statement> insertStatementStream = objectValueToInsertStatementStream(fieldDefinitionContext, objectValue, level + 1, index);

        Stream<Statement> objectInsertStatementStream = inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValue, inputValueDefinitionContext)
                                                                .map(objectFieldContext ->
                                                                        objectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                objectFieldContext.value().objectValue(),
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                level + 1,
                                                                                index
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listObjectInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValue, inputValueDefinitionContext)
                                                                .map(objectFieldContext ->
                                                                        listObjectValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                objectFieldContext.value().arrayValue(),
                                                                                level + 1
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListObjectValueToStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputObjectTypeDefinitionContext,
                                                                                inputValueDefinitionContext,
                                                                                level + 1
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        Stream<Statement> listInsertStatementStream = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .map(inputValueDefinitionContext ->
                        manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext)
                                .flatMap(subFieldDefinitionContext ->
                                        manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))
                                                .map(inputObjectTypeDefinitionContext ->
                                                        manager.getObjectFieldFromInputValueDefinition(objectValue, inputValueDefinitionContext)
                                                                .map(objectFieldContext ->
                                                                        listValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                objectFieldContext.value().arrayValue()
                                                                        )
                                                                )
                                                                .orElse(
                                                                        defaultListValueToInsertStatementStream(
                                                                                fieldDefinitionContext,
                                                                                idValueExpression,
                                                                                subFieldDefinitionContext,
                                                                                inputValueDefinitionContext
                                                                        )
                                                                )
                                                )
                                )
                ).filter(Optional::isPresent)
                .flatMap(Optional::get);

        return Stream.concat(updateMapObjectFieldStatementStream, Stream.concat(insertStatementStream, Stream.concat(objectInsertStatementStream, Stream.concat(listObjectInsertStatementStream, listInsertStatementStream))));
    }

    protected Stream<Statement> defaultValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                              Expression parentIdValueExpression,
                                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                              GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                              GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext,
                                                              int level,
                                                              int index) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                return objectValueToStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinition,
                        parentInputValueDefinitionContext.defaultValue().value().objectValue(),
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
                                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                                   GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext,
                                                                                   int level) {

        Stream<Statement> listArgumentInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> objectValueWithVariableToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinition,
                        arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable(),
                        level,
                        index
                ))
                .flatMap(statementStream -> statementStream);

        Stream<Statement> listArgumentDeleteStatementStream = mapObjectFieldDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                        .mapToObj(index ->
                                manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), arrayValueWithVariableContext.valueWithVariable(index).objectValueWithVariable())
                                        .map(this::createIdValueExpression)
                                        .orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index)))
                        .collect(Collectors.toList())
        );

        return Stream.concat(listArgumentInsertStatementStream, listArgumentDeleteStatementStream);
    }

    protected Stream<Statement> listObjectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                       Expression parentIdValueExpression,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                       GraphqlParser.ArrayValueContext arrayValueContext,
                                                                       int level) {

        Stream<Statement> listArgumentInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> objectValueToStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinition,
                        arrayValueContext.value(index).objectValue(),
                        level,
                        index
                ))
                .flatMap(statementStream -> statementStream);

        Stream<Statement> listArgumentDeleteStatementStream = mapObjectFieldDeleteStatementStream(
                parentFieldDefinitionContext,
                parentIdValueExpression,
                fieldDefinitionContext,
                IntStream.range(0, arrayValueContext.value().size())
                        .mapToObj(index ->
                                manager.getIDObjectField(fieldDefinitionContext.type(), arrayValueContext.value(index).objectValue())
                                        .map(this::createIdValueExpression)
                                        .orElse(createInsertIdUserVariable(fieldDefinitionContext, level, index)))
                        .collect(Collectors.toList())
        );

        return Stream.concat(listArgumentInsertStatementStream, listArgumentDeleteStatementStream);
    }

    protected Stream<Statement> defaultListObjectValueToStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                        Expression parentIdValueExpression,
                                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                        GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                        GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext,
                                                                        int level) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                return listObjectValueToInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        inputObjectTypeDefinition,
                        parentInputValueDefinitionContext.defaultValue().value().arrayValue(),
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
                                                                             GraphqlParser.ArrayValueWithVariableContext arrayValueWithVariableContext) {

        Stream<Statement> listValueDeleteStatementStream = mapFieldDeleteStatementStream(parentFieldDefinitionContext, parentIdValueExpression, fieldDefinitionContext);

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueWithVariableContext.valueWithVariable().size())
                .mapToObj(index -> mapFieldInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        valueWithVariableToDBValue(arrayValueWithVariableContext.valueWithVariable(index))
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listValueDeleteStatementStream, listValueInsertStatementStream);
    }


    protected Stream<Statement> listValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                 Expression parentIdValueExpression,
                                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArrayValueContext arrayValueContext) {

        Stream<Statement> listValueDeleteStatementStream = mapFieldDeleteStatementStream(parentFieldDefinitionContext, parentIdValueExpression, fieldDefinitionContext);

        Stream<Statement> listValueInsertStatementStream = IntStream.range(0, arrayValueContext.value().size())
                .mapToObj(index -> mapFieldInsertStatementStream(
                        parentFieldDefinitionContext,
                        parentIdValueExpression,
                        fieldDefinitionContext,
                        valueToDBValue(arrayValueContext.value(index))
                        )
                )
                .flatMap(statementStream -> statementStream);

        return Stream.concat(listValueDeleteStatementStream, listValueInsertStatementStream);
    }


    protected Stream<Statement> defaultListValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                        Expression parentIdValueExpression,
                                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                        GraphqlParser.InputValueDefinitionContext parentInputValueDefinitionContext) {

        if (parentInputValueDefinitionContext.type().nonNullType() != null) {
            if (parentInputValueDefinitionContext.defaultValue() != null) {
                Stream<Statement> listValueDeleteStatementStream = mapFieldDeleteStatementStream(parentFieldDefinitionContext, parentIdValueExpression, fieldDefinitionContext);

                Stream<Statement> listValueInsertStatementStream = IntStream.range(0, parentInputValueDefinitionContext.defaultValue().value().arrayValue().value().size())
                        .mapToObj(index -> mapFieldInsertStatementStream(
                                parentFieldDefinitionContext,
                                parentIdValueExpression,
                                fieldDefinitionContext,
                                valueToDBValue(parentInputValueDefinitionContext.defaultValue().value().arrayValue().value(index))
                                )
                        )
                        .flatMap(statementStream -> statementStream);

                return Stream.concat(listValueDeleteStatementStream, listValueInsertStatementStream);
            } else {
                //TODO
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> mapObjectFieldUpdateStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                    Expression parentIdValueExpression,
                                                                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                    Expression idValueExpression) {

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
            Column parentColumn = fieldToColumn(parentTable, fromFieldDefinition.get());
            Column column = fieldToColumn(table, toFieldDefinition.get());
            Column parentIdColumn = fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column idColumn = fieldToColumn(table, idFieldDefinition.get());
            EqualsTo parentIdEqualsTo = new EqualsTo();
            parentIdEqualsTo.setLeftExpression(parentIdColumn);
            parentIdEqualsTo.setRightExpression(parentIdValueExpression);
            EqualsTo idEqualsTo = new EqualsTo();
            idEqualsTo.setLeftExpression(idColumn);
            idEqualsTo.setRightExpression(idValueExpression);

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = new Table(mapWithTypeName.get());
                    Column withParentColumn = new Column(withTable, mapWithFromFieldName.get());
                    Column withColumn = new Column(withTable, mapWithToFieldName.get());
                    SelectExpressionItem withParentColumnExpression = new SelectExpressionItem(withParentColumn);
                    SelectExpressionItem withColumnExpression = new SelectExpressionItem(withColumn);

                    Select select = new Select();
                    PlainSelect plainSelect = new PlainSelect();
                    plainSelect.setFromItem(withTable);
                    plainSelect.setSelectItems(Arrays.asList(withParentColumnExpression, withColumnExpression));

                    Join joinParentTable = new Join();
                    EqualsTo joinTableEqualsParentColumn = new EqualsTo();
                    joinTableEqualsParentColumn.setLeftExpression(withParentColumn);
                    joinTableEqualsParentColumn.setRightExpression(parentColumn);

                    Join joinTable = new Join();
                    EqualsTo joinTableEqualsColumn = new EqualsTo();
                    joinTableEqualsColumn.setLeftExpression(withColumn);
                    joinTableEqualsColumn.setRightExpression(column);

                    plainSelect.setJoins(Arrays.asList(joinParentTable, joinTable));

                    MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(parentIdEqualsTo, idEqualsTo));
                    plainSelect.setWhere(multiAndExpression);
                    select.setSelectBody(plainSelect);
                    return Stream.of(insertSelectExpression(withTable, Arrays.asList(withParentColumn, withColumn), select));
                }
            } else {
                SubSelect selectParentColumn = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);
                SubSelect selectColumn = selectFieldByIdExpression(table, column, idColumn, idValueExpression);

                return Stream.of(
                        updateExpression(parentTable, Collections.singletonList(parentColumn), Collections.singletonList(selectColumn), parentIdEqualsTo),
                        updateExpression(table, Collections.singletonList(column), Collections.singletonList(selectParentColumn), idEqualsTo)
                );
            }
        }

        return Stream.empty();
    }

    protected Stream<Statement> mapObjectFieldDeleteStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                                    Expression parentIdValueExpression,
                                                                    GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                    List<Expression> idValueExpressionList) {

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
            Column parentColumn = fieldToColumn(parentTable, fromFieldDefinition.get());
            Column column = fieldToColumn(table, toFieldDefinition.get());
            Column parentIdColumn = fieldToColumn(parentTable, parentIdFieldDefinition.get());
            Column idColumn = fieldToColumn(table, idFieldDefinition.get());

            EqualsTo parentIdEqualsTo = new EqualsTo();
            parentIdEqualsTo.setLeftExpression(parentIdColumn);
            parentIdEqualsTo.setRightExpression(parentIdValueExpression);

            InExpression idNotIn = new InExpression();
            idNotIn.setLeftExpression(idColumn);
            idNotIn.setNot(true);
            idNotIn.setRightItemsList(new ExpressionList(idValueExpressionList));

            MultiAndExpression equalsParentIdAndIdNotIn = new MultiAndExpression(Arrays.asList(parentIdEqualsTo, idNotIn));

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = new Table(mapWithTypeName.get());
                    Column withParentColumn = new Column(withTable, mapWithFromFieldName.get());
                    Column withColumn = new Column(withTable, mapWithToFieldName.get());

                    Join joinParentTable = new Join();
                    EqualsTo joinTableEqualsParentColumn = new EqualsTo();
                    joinTableEqualsParentColumn.setLeftExpression(withParentColumn);
                    joinTableEqualsParentColumn.setRightExpression(parentColumn);

                    Join joinTable = new Join();
                    EqualsTo joinTableEqualsColumn = new EqualsTo();
                    joinTableEqualsColumn.setLeftExpression(withColumn);
                    joinTableEqualsColumn.setRightExpression(column);

                    return Stream.of(deleteWithJoinExpression(withTable, Arrays.asList(joinParentTable, joinTable), equalsParentIdAndIdNotIn));
                }
            } else {

                return Stream.of(deleteExpression(table, equalsParentIdAndIdNotIn));
            }
        }
        return Stream.empty();
    }


    protected Stream<Statement> mapFieldDeleteStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                              Expression parentIdValueExpression,
                                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);

        if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {

            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);
            Table parentTable = typeToTable(parentFieldDefinitionContext);
            Column parentColumn = fieldToColumn(parentTable, fromFieldDefinition.get());
            Column parentIdColumn = fieldToColumn(parentTable, parentIdFieldDefinition.get());

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent()) {
                    Table withTable = new Table(mapWithTypeName.get());
                    Column withParentColumn = new Column(withTable, mapWithFromFieldName.get());

                    SubSelect selectParentColumn = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);

                    EqualsTo columnEqualsTo = new EqualsTo();
                    columnEqualsTo.setLeftExpression(withParentColumn);
                    columnEqualsTo.setRightExpression(selectParentColumn);

                    return Stream.of(deleteExpression(withTable, columnEqualsTo));
                }
            }
        }
        return Stream.empty();
    }


    protected Stream<Statement> mapFieldInsertStatementStream(GraphqlParser.FieldDefinitionContext parentFieldDefinitionContext,
                                                              Expression parentIdValueExpression,
                                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                              Expression valueExpression) {

        String parentFieldTypeName = manager.getFieldTypeName(parentFieldDefinitionContext.type());
        Optional<GraphqlParser.FieldDefinitionContext> parentIdFieldDefinition = manager.getObjectTypeIDFieldDefinition(parentFieldTypeName);
        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(parentFieldTypeName, fieldDefinitionContext);

        if (fromFieldDefinition.isPresent() && parentIdFieldDefinition.isPresent()) {

            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(fieldDefinitionContext);
            Table parentTable = typeToTable(parentFieldDefinitionContext);
            Column parentColumn = fieldToColumn(parentTable, fromFieldDefinition.get());
            Column parentIdColumn = fieldToColumn(parentTable, parentIdFieldDefinition.get());

            if (mapWithTypeArgument.isPresent()) {
                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                    Table withTable = new Table(mapWithTypeName.get());
                    Column withParentColumn = new Column(withTable, mapWithFromFieldName.get());
                    Column withColumn = new Column(withTable, mapWithToFieldName.get());

                    SubSelect selectParentColumn = selectFieldByIdExpression(parentTable, parentColumn, parentIdColumn, parentIdValueExpression);

                    return Stream.of(insertExpression(withTable, Arrays.asList(withParentColumn, withColumn), new ExpressionList(Arrays.asList(selectParentColumn, valueExpression))));
                }
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
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = argumentsToInsert(table, fieldList, argumentsContext);

        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), 0, 0));
        }
        return Stream.of(insert);
    }

    protected Stream<Statement> objectValueWithVariableToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               int level,
                                                                               int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = objectValueWithVariableToInsert(table, fieldList, objectValueWithVariableContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariable.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
        }

        return Stream.of(insert);
    }

    protected Stream<Statement> objectValueToInsertStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext,
                                                                   int level,
                                                                   int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = typeToTable(fieldDefinitionContext);

        List<GraphqlParser.InputValueDefinitionContext> fieldList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> !manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = objectValueToInsert(table, fieldList, objectValueContext);
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        Optional<GraphqlParser.ObjectFieldContext> idObjectField = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);
        if (idObjectField.isEmpty() && idFieldName.isPresent()) {
            return Stream.of(insert, DB_VALUE_UTIL.createInsertIdSetStatement(typeName, idFieldName.get(), level, index));
        }

        return Stream.of(insert);
    }

    protected Insert argumentsToInsert(Table table,
                                       List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                       GraphqlParser.ArgumentsContext argumentsContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                .map(argumentContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), argumentContext))
                                .orElse(fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                ).collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext)
                                        .map(argumentContext -> argumentToDBValue(inputValueDefinitionContext, argumentContext))
                                        .orElse(argumentToDBValue(inputValueDefinitionContext))
                        ).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }


    protected Insert objectValueWithVariableToInsert(Table table,
                                                     List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                     GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                .map(objectFieldWithVariableContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), objectFieldWithVariableContext))
                                .orElse(fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                ).collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext)
                                        .map(objectFieldWithVariableContext -> objectFieldWithVariableToDBValue(inputValueDefinitionContext, objectFieldWithVariableContext))
                                        .orElse(argumentToDBValue(inputValueDefinitionContext))
                        ).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }


    protected Insert objectValueToInsert(Table table,
                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                         GraphqlParser.ObjectValueContext objectValueContext) {


        List<Column> columnList = inputValueDefinitionContextList.stream()
                .map(inputValueDefinitionContext ->
                        manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                .map(objectFieldContext -> fieldToColumn(typeToTable(inputValueDefinitionContext), objectFieldContext))
                                .orElse(fieldToColumn(typeToTable(inputValueDefinitionContext), inputValueDefinitionContext))
                ).collect(Collectors.toList());

        ExpressionList expressionList = new ExpressionList(
                inputValueDefinitionContextList.stream()
                        .map(inputValueDefinitionContext ->
                                manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext)
                                        .map(objectFieldContext -> objectFieldToDBValue(inputValueDefinitionContext, objectFieldContext))
                                        .orElse(argumentToDBValue(inputValueDefinitionContext))
                        ).collect(Collectors.toList())
        );
        return insertExpression(table, columnList, expressionList);
    }

    protected Expression argumentToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(argumentContext.valueWithVariable());
        }
        return null;
    }

    protected Expression objectFieldWithVariableToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(objectFieldWithVariableContext.valueWithVariable());
        }
        return null;
    }

    protected Expression objectFieldToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectFieldContext objectFieldContext) {
        if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.value());
        } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            return DB_VALUE_UTIL.enumValueToDBValue(objectFieldContext.value());
        }
        return null;
    }

    protected Expression argumentToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
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

    protected Table typeToTable(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return typeToTable(inputValueDefinitionContext.type());
    }

    protected Table typeToTable(GraphqlParser.TypeContext typeContext) {
        return new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(typeContext)));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldDefinitionContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ArgumentContext argumentContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(argumentContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldWithVariableContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.ObjectFieldContext objectFieldContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldContext.name().getText()));
    }

    protected Column fieldToColumn(Table table, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
    }

    protected Expression createIdValueExpression(GraphqlParser.ArgumentContext idArgumentContext) {
        return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(idArgumentContext.valueWithVariable());
    }

    protected Expression createIdValueExpression(GraphqlParser.ObjectFieldWithVariableContext objectIdFieldWithVariableContext) {
        return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectIdFieldWithVariableContext.valueWithVariable());
    }

    protected Expression createIdValueExpression(GraphqlParser.ObjectFieldContext objectIdFieldContext) {
        return DB_VALUE_UTIL.scalarValueToDBValue(objectIdFieldContext.value());
    }

    public UserVariable createInsertIdUserVariable(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level, int index) {
        String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(typeName);
        return idFieldName.map(s -> DB_VALUE_UTIL.createInsertIdUserVariable(typeName, s, level, index)).orElse(null);
    }

    public Expression valueWithVariableToDBValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.enumValue() != null) {
            return DB_VALUE_UTIL.enumValueWithVariableToDBValue(valueWithVariableContext);
        } else {
            return DB_VALUE_UTIL.scalarValueWithVariableToDBValue(valueWithVariableContext);
        }
    }

    public Expression valueToDBValue(GraphqlParser.ValueContext valueContext) {
        if (valueContext.enumValue() != null) {
            return DB_VALUE_UTIL.enumValueToDBValue(valueContext);
        } else {
            return DB_VALUE_UTIL.scalarValueToDBValue(valueContext);
        }
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

    protected Insert insertSelectExpression(Table table,
                                            List<Column> columnList,
                                            Select select) {

        Insert insert = new Insert();
        insert.setTable(table);
        insert.setColumns(columnList);
        insert.setSelect(select);
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


    protected Delete deleteWithJoinExpression(Table table, List<Join> joinList, Expression where) {

        Delete delete = new Delete();
        delete.setTable(table);
        delete.setJoins(joinList);
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