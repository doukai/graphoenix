package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.*;
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
                Select select = new Select();
                PlainSelect body = new PlainSelect();
                SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
                selectExpressionItem.setExpression(graphqlQueryToSelect.objectFieldSelectionToJsonObjectFunction(mutationOperationTypeDefinition.get().typeName().name().getText(), operationDefinitionContext.selectionSet().selection()));
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
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(mutationTypeName -> manager.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText()));
        if (mutationFieldTypeDefinitionContext.isPresent()) {
            return argumentsToStatementStream(mutationFieldTypeDefinitionContext.get(), selectionContext.field().arguments());
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext) {
        return argumentsToStatementStream(fieldDefinitionContext, argumentsContext, 0);
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext,
                                                           int level) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarArgumentsToInsert(fieldDefinitionContext, argumentsContext);
        statementList.add(insert);

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName, level)));
        }

        fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .forEach(inputValueDefinitionContext -> {

                    Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                    Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                    if (subFieldDefinitionContext.isPresent() && inputObjectTypeDefinition.isPresent()) {
                        String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                        if (argumentContext.isPresent()) {
                            statementList.addAll(
                                    singleTypeObjectValueWithVariableToStatementStream(
                                            subFieldDefinitionContext.get(),
                                            inputObjectTypeDefinition.get(),
                                            argumentContext.get().valueWithVariable().objectValueWithVariable(),
                                            level + 1
                                    ).collect(Collectors.toList()));

                            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), argumentContext.get().valueWithVariable().objectValueWithVariable());
                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.fieldDefinitionMapWithTypeArgument(subFieldDefinitionContext.get());
                                if (mapWithTypeArgument.isPresent()) {
                                    Optional<String> mapWithTypeName = manager.getMappingToFieldDefinitionWithTypeName(mapWithTypeArgument.get());
                                    Optional<String> mapWithFromFieldName = manager.getMappingToFieldDefinitionWithTypeFromFieldName(mapWithTypeArgument.get());
                                    Optional<String> mapWithToFieldName = manager.getMappingToFieldDefinitionWithTypeToFieldName(mapWithTypeArgument.get());
                                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                        statementList.add(mapWithTypeInsert(
                                                mapWithTypeName.get(),
                                                mapWithFromFieldName.get(),
                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                mapWithToFieldName.get(),
                                                objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                        ));
                                    }
                                } else {
                                    statementList.add(mapColumnUpdate(
                                            mappingFromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                            mappingToFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                    ));

                                    statementList.add(mapColumnUpdate(
                                            mappingToFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                            mappingFromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                    ));
                                }
                            }
                        } else {
                            if (inputValueDefinitionContext.type().nonNullType() != null) {
                                if (inputValueDefinitionContext.defaultValue() != null) {
                                    statementList.addAll(
                                            singleTypeObjectValueToStatementStream(
                                                    subFieldDefinitionContext.get(),
                                                    inputObjectTypeDefinition.get(),
                                                    inputValueDefinitionContext.defaultValue().value().objectValue(),
                                                    level + 1
                                            ).collect(Collectors.toList()));

                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), inputValueDefinitionContext.defaultValue().value().objectValue());
                                    if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                        Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.fieldDefinitionMapWithTypeArgument(subFieldDefinitionContext.get());
                                        if (mapWithTypeArgument.isPresent()) {
                                            Optional<String> mapWithTypeName = manager.getMappingToFieldDefinitionWithTypeName(mapWithTypeArgument.get());
                                            Optional<String> mapWithFromFieldName = manager.getMappingToFieldDefinitionWithTypeFromFieldName(mapWithTypeArgument.get());
                                            Optional<String> mapWithToFieldName = manager.getMappingToFieldDefinitionWithTypeToFieldName(mapWithTypeArgument.get());
                                            if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                statementList.add(mapWithTypeInsert(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                        mapWithToFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                ));
                                            }
                                        } else {
                                            statementList.add(mapColumnUpdate(
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                            ));

                                            statementList.add(mapColumnUpdate(
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                            ));
                                        }
                                    }
                                } else {
                                    //TODO
                                }
                            }
                        }
                    }
                });

        fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .forEach(inputValueDefinitionContext -> {
                    if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                        if (subFieldDefinitionContext.isPresent() && inputObjectTypeDefinition.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);
                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                if (argumentContext.isPresent()) {
                                    Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.fieldDefinitionMapWithTypeArgument(subFieldDefinitionContext.get());
                                    List<GraphqlParser.ObjectFieldWithVariableContext> updateObjectFieldWithVariableContextList = new ArrayList<>();

                                    if (mapWithTypeArgument.isPresent()) {
                                        Optional<String> mapWithTypeName = manager.getMappingToFieldDefinitionWithTypeName(mapWithTypeArgument.get());
                                        Optional<String> mapWithFromFieldName = manager.getMappingToFieldDefinitionWithTypeFromFieldName(mapWithTypeArgument.get());
                                        Optional<String> mapWithToFieldName = manager.getMappingToFieldDefinitionWithTypeToFieldName(mapWithTypeArgument.get());
                                        if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                            argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().forEach(valueWithVariableContext -> {

                                                statementList.addAll(
                                                        singleTypeObjectValueWithVariableToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                inputObjectTypeDefinition.get(),
                                                                valueWithVariableContext.objectValueWithVariable(),
                                                                level + 1
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                                objectIdFieldWithVariableContext.ifPresent(updateObjectFieldWithVariableContextList::add);

                                                statementList.add(mapWithTypeInsert(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                        mapWithToFieldName.get(),
                                                        objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                ));
                                            });
                                            statementList.add(mapWithTypeDelete(
                                                    mapWithTypeName.get(),
                                                    mapWithToFieldName.get(),
                                                    updateObjectFieldWithVariableContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                    mapWithFromFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                            ));
                                        }
                                    } else {
                                        argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().forEach(valueWithVariableContext -> {

                                            statementList.addAll(
                                                    singleTypeObjectValueWithVariableToStatementStream(
                                                            subFieldDefinitionContext.get(),
                                                            inputObjectTypeDefinition.get(),
                                                            valueWithVariableContext.objectValueWithVariable(),
                                                            level + 1
                                                    ).collect(Collectors.toList()));

                                            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                            objectIdFieldWithVariableContext.ifPresent(updateObjectFieldWithVariableContextList::add);

                                            statementList.add(mapColumnUpdate(
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                            ));

                                            statementList.add(mapColumnUpdate(
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                            ));

                                        });

                                        statementList.add(mapColumnDelete(
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                updateObjectFieldWithVariableContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                        ));

                                    }
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.fieldDefinitionMapWithTypeArgument(subFieldDefinitionContext.get());
                                            List<GraphqlParser.ObjectFieldContext> updateObjectFieldContextList = new ArrayList<>();
                                            if (mapWithTypeArgument.isPresent()) {
                                                Optional<String> mapWithTypeName = manager.getMappingToFieldDefinitionWithTypeName(mapWithTypeArgument.get());
                                                Optional<String> mapWithFromFieldName = manager.getMappingToFieldDefinitionWithTypeFromFieldName(mapWithTypeArgument.get());
                                                Optional<String> mapWithToFieldName = manager.getMappingToFieldDefinitionWithTypeToFieldName(mapWithTypeArgument.get());
                                                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                    inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {
                                                        statementList.addAll(
                                                                singleTypeObjectValueToStatementStream(
                                                                        subFieldDefinitionContext.get(),
                                                                        inputObjectTypeDefinition.get(),
                                                                        valueContext.objectValue(),
                                                                        level + 1
                                                                ).collect(Collectors.toList()));

                                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                        objectIdFieldContext.ifPresent(updateObjectFieldContextList::add);

                                                        statementList.add(mapWithTypeInsert(
                                                                mapWithTypeName.get(),
                                                                mapWithFromFieldName.get(),
                                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                                mapWithToFieldName.get(),
                                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                        ));
                                                    });
                                                    statementList.add(mapWithTypeDelete(
                                                            mapWithTypeName.get(),
                                                            mapWithToFieldName.get(),
                                                            updateObjectFieldContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                            mapWithFromFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                                    ));
                                                }
                                            } else {
                                                inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {
                                                    statementList.addAll(
                                                            singleTypeObjectValueToStatementStream(
                                                                    subFieldDefinitionContext.get(),
                                                                    inputObjectTypeDefinition.get(),
                                                                    valueContext.objectValue(),
                                                                    level + 1
                                                            ).collect(Collectors.toList()));

                                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                    objectIdFieldContext.ifPresent(updateObjectFieldContextList::add);

                                                    statementList.add(mapColumnUpdate(
                                                            mappingFromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                            mappingToFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                    ));
                                                    statementList.add(mapColumnUpdate(
                                                            mappingToFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                            mappingFromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                                    ));
                                                });
                                                statementList.add(mapColumnDelete(
                                                        mappingToFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        updateObjectFieldContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                        mappingFromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                                ));
                                            }
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        //TODO
                    }
                });
        return statementList.stream();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                   int level) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarInputValuesToInsert(fieldDefinitionContext, inputObjectTypeDefinition, objectValueWithVariableContext);
        statementList.add(insert);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariableContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName, level)));
        }

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .forEach(inputValueDefinitionContext -> {

                    Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                    Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                    if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                        String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                        if (objectFieldWithVariableContext.isPresent()) {
                            statementList.addAll(
                                    singleTypeObjectValueWithVariableToStatementStream(
                                            subFieldDefinitionContext.get(),
                                            subInputObjectTypeDefinition.get(),
                                            objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable(),
                                            level + 1
                                    ).collect(Collectors.toList()));

                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                                statementList.add(mapColumnUpdate(
                                        mappingFromFieldDefinition.get().name().getText(),
                                        fieldTypeName,
                                        idFieldName.get(),
                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                        mappingToFieldDefinition.get().name().getText(),
                                        subFieldTypeName,
                                        subIdFieldName.get(),
                                        objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                ));

                                statementList.add(mapColumnUpdate(
                                        mappingToFieldDefinition.get().name().getText(),
                                        subFieldTypeName,
                                        subIdFieldName.get(),
                                        objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                        mappingFromFieldDefinition.get().name().getText(),
                                        fieldTypeName,
                                        idFieldName.get(),
                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                ));
                            }
                        } else {
                            if (inputValueDefinitionContext.type().nonNullType() != null) {
                                if (inputValueDefinitionContext.defaultValue() != null) {
                                    statementList.addAll(
                                            singleTypeObjectValueToStatementStream(
                                                    subFieldDefinitionContext.get(),
                                                    subInputObjectTypeDefinition.get(),
                                                    inputValueDefinitionContext.defaultValue().value().objectValue(),
                                                    level + 1
                                            ).collect(Collectors.toList()));

                                    if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), inputValueDefinitionContext.defaultValue().value().objectValue());
                                        statementList.add(mapColumnUpdate(
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                        ));

                                        statementList.add(mapColumnUpdate(
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                        ));
                                    }
                                } else {
                                    //TODO
                                }
                            }
                        }
                    }
                });

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .forEach(inputValueDefinitionContext -> {
                    if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                        Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                        if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                if (objectFieldWithVariableContext.isPresent()) {
                                    List<GraphqlParser.ObjectFieldWithVariableContext> updateObjectFieldWithVariableContextList = new ArrayList<>();
                                    objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().forEach(valueWithVariableContext -> {

                                        statementList.addAll(
                                                singleTypeObjectValueWithVariableToStatementStream(
                                                        subFieldDefinitionContext.get(),
                                                        subInputObjectTypeDefinition.get(),
                                                        valueWithVariableContext.objectValueWithVariable(),
                                                        level + 1
                                                ).collect(Collectors.toList()));

                                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                        objectIdFieldWithVariableContext.ifPresent(updateObjectFieldWithVariableContextList::add);

                                        statementList.add(mapColumnUpdate(
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                        ));

                                        statementList.add(mapColumnUpdate(
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                        ));
                                    });

                                    statementList.add(mapColumnDelete(
                                            mappingToFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            updateObjectFieldWithVariableContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                            mappingFromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                    ));
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            List<GraphqlParser.ObjectFieldContext> updateObjectFieldContextList = new ArrayList<>();
                                            inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {

                                                statementList.addAll(
                                                        singleTypeObjectValueToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                subInputObjectTypeDefinition.get(),
                                                                valueContext.objectValue(),
                                                                level + 1
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                objectIdFieldContext.ifPresent(updateObjectFieldContextList::add);

                                                statementList.add(mapColumnUpdate(
                                                        mappingFromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                        mappingToFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                ));

                                                statementList.add(mapColumnUpdate(
                                                        mappingToFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                        mappingFromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                                ));
                                            });

                                            statementList.add(mapColumnDelete(
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    updateObjectFieldContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    objectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                            ));
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                    }
                });

        return statementList.stream();
    }

    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                       GraphqlParser.ObjectValueContext objectValueContext,
                                                                       int level) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarInputValuesToInsert(fieldDefinitionContext, inputObjectTypeDefinition, objectValueContext);
        statementList.add(insert);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());

        Optional<GraphqlParser.ObjectFieldContext> idObjectFieldContext = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);
        if (idObjectFieldContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName, level)));
        }

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                .forEach(inputValueDefinitionContext -> {

                    Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                    Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                    if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                        String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                        if (objectFieldContext.isPresent()) {
                            statementList.addAll(
                                    singleTypeObjectValueToStatementStream(
                                            subFieldDefinitionContext.get(),
                                            subInputObjectTypeDefinition.get(),
                                            objectFieldContext.get().value().objectValue(),
                                            level + 1
                                    ).collect(Collectors.toList()));

                            Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), objectFieldContext.get().value().objectValue());

                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                statementList.add(mapColumnUpdate(
                                        mappingFromFieldDefinition.get().name().getText(),
                                        fieldTypeName,
                                        idFieldName.get(),
                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                        mappingToFieldDefinition.get().name().getText(),
                                        subFieldTypeName,
                                        subIdFieldName.get(),
                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                ));

                                statementList.add(mapColumnUpdate(
                                        mappingToFieldDefinition.get().name().getText(),
                                        subFieldTypeName,
                                        subIdFieldName.get(),
                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                        mappingFromFieldDefinition.get().name().getText(),
                                        fieldTypeName,
                                        idFieldName.get(),
                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                ));
                            }
                        } else {
                            if (inputValueDefinitionContext.type().nonNullType() != null) {
                                if (inputValueDefinitionContext.defaultValue() != null) {
                                    statementList.addAll(
                                            singleTypeObjectValueToStatementStream(
                                                    subFieldDefinitionContext.get(),
                                                    subInputObjectTypeDefinition.get(),
                                                    inputValueDefinitionContext.defaultValue().value().objectValue(),
                                                    level + 1
                                            ).collect(Collectors.toList()));

                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), inputValueDefinitionContext.defaultValue().value().objectValue());
                                    if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                        statementList.add(mapColumnUpdate(
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                        ));

                                        statementList.add(mapColumnUpdate(
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                        ));
                                    }
                                } else {
                                    //TODO
                                }
                            }
                        }
                    }
                });

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .forEach(inputValueDefinitionContext -> {
                    if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                        Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                        if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> mappingFromFieldDefinition = manager.getMappingFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> mappingToFieldDefinition = manager.getMappingToFieldDefinition(subFieldDefinitionContext.get());
                            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                            if (mappingFromFieldDefinition.isPresent() && mappingToFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                if (objectFieldContext.isPresent()) {
                                    List<GraphqlParser.ObjectFieldContext> updateObjectFieldContextList = new ArrayList<>();
                                    objectFieldContext.get().value().arrayValue().value().forEach(valueContext -> {

                                        statementList.addAll(
                                                singleTypeObjectValueToStatementStream(
                                                        subFieldDefinitionContext.get(),
                                                        subInputObjectTypeDefinition.get(),
                                                        valueContext.objectValue(),
                                                        level + 1
                                                ).collect(Collectors.toList()));

                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                        objectIdFieldContext.ifPresent(updateObjectFieldContextList::add);

                                        statementList.add(mapColumnUpdate(
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                        ));

                                        statementList.add(mapColumnUpdate(
                                                mappingToFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                mappingFromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                        ));
                                    });

                                    statementList.add(mapColumnDelete(
                                            mappingToFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            updateObjectFieldContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                            mappingFromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                    ));
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            List<GraphqlParser.ObjectFieldContext> updateObjectFieldContextList = new ArrayList<>();
                                            inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {
                                                statementList.addAll(
                                                        singleTypeObjectValueToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                subInputObjectTypeDefinition.get(),
                                                                valueContext.objectValue(),
                                                                level + 1
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                objectIdFieldContext.ifPresent(updateObjectFieldContextList::add);

                                                statementList.add(mapColumnUpdate(
                                                        mappingFromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level)),
                                                        mappingToFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1))
                                                ));

                                                statementList.add(mapColumnUpdate(
                                                        mappingToFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1)),
                                                        mappingFromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                                ));
                                            });

                                            statementList.add(mapColumnDelete(
                                                    mappingToFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    updateObjectFieldContextList.stream().map(this::createIdValueExpression).collect(Collectors.toList()),
                                                    mappingFromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    objectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level))
                                            ));
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                    }
                });

        return statementList.stream();
    }

    protected Update mapColumnUpdate(String mapFromFieldName,
                                     String mapFromFieldTypeName,
                                     String mapFromIdFieldName,
                                     Expression mapFromIdValueExpression,
                                     String mapToFieldName,
                                     String mapToFieldTypeName,
                                     String mapToIdFieldName,
                                     Expression mapToIdValueExpression) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapToFieldTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToFieldName)));
        selectExpressionItem.setAlias(new Alias(mapToFieldName));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToIdFieldName)));
        subEqualsTo.setRightExpression(mapToIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Update update = new Update();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapFromFieldTypeName);
        Table table = new Table(tableName);
        update.setTable(table);
        Column updateColumn = new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromFieldName));
        update.setColumns(Collections.singletonList(updateColumn));
        update.setExpressions(Collections.singletonList(subSelect));
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromIdFieldName)));
        equalsTo.setRightExpression(mapFromIdValueExpression);
        IsNullExpression isNullExpression = new IsNullExpression();
        isNullExpression.setLeftExpression(updateColumn);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(isNullExpression, equalsTo));
        update.setWhere(multiAndExpression);
        return update;
    }

    protected Delete mapWithTypeDelete(String mapWithTypeName,
                                       String mapFromFieldName,
                                       List<Expression> mapFromIdValueExpressionList,
                                       String mapToFieldName,
                                       Expression mapToIdValueExpression) {
        Delete delete = new Delete();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapWithTypeName);
        Table table = new Table(tableName);

        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToFieldName)));
        equalsTo.setRightExpression(mapToIdValueExpression);
        if (mapFromIdValueExpressionList.size() > 0) {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromFieldName)));
            inExpression.setNot(true);
            inExpression.setRightItemsList(new ExpressionList(mapFromIdValueExpressionList));
            delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        } else {
            delete.setWhere(equalsTo);
        }
        return delete;
    }

    protected Insert mapWithTypeInsert(String mapWithTypeName,
                                       String mapFromFieldName,
                                       Expression mapFromIdValueExpression,
                                       String mapToFieldName,
                                       Expression mapToIdValueExpression) {
        Insert insert = new Insert();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapWithTypeName);
        Table table = new Table(tableName);
        insert.setTable(table);

        Select select = new Select();
        PlainSelect plainSelect = new PlainSelect();

        SelectExpressionItem mapFromIdValueSelectExpression = new SelectExpressionItem();
        mapFromIdValueSelectExpression.setExpression(mapFromIdValueExpression);
        SelectExpressionItem mapToIdValueSelectExpression = new SelectExpressionItem();
        mapToIdValueSelectExpression.setExpression(mapToIdValueExpression);
        plainSelect.setSelectItems(Arrays.asList(mapFromIdValueSelectExpression, mapToIdValueSelectExpression));

        ExistsExpression existsExpression = new ExistsExpression();
        existsExpression.setNot(true);

        SubSelect subSelect = new SubSelect();
        PlainSelect existsSelect = new PlainSelect();
        existsSelect.setSelectItems(Collections.singletonList(new AllColumns()));

        EqualsTo fromFieldEqualsTo = new EqualsTo();
        fromFieldEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromFieldName)));
        fromFieldEqualsTo.setRightExpression(mapFromIdValueExpression);
        EqualsTo toFieldEqualsTo = new EqualsTo();
        toFieldEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToFieldName)));
        toFieldEqualsTo.setRightExpression(mapToIdValueExpression);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(fromFieldEqualsTo, toFieldEqualsTo));
        existsSelect.setWhere(multiAndExpression);
        existsSelect.setFromItem(table);
        subSelect.setSelectBody(existsSelect);
        existsExpression.setRightExpression(subSelect);
        plainSelect.setWhere(existsExpression);
        select.setSelectBody(plainSelect);

        insert.setSelect(select);

        return insert;
    }

    protected Delete mapColumnDelete(String mapFromFieldName,
                                     String mapFromFieldTypeName,
                                     String mapFromIdFieldName,
                                     List<Expression> mapFromIdValueExpressionList,
                                     String mapToFieldName,
                                     String mapToFieldTypeName,
                                     String mapToIdFieldName,
                                     Expression mapToIdValueExpression) {

        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapToFieldTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToFieldName)));
        selectExpressionItem.setAlias(new Alias(mapToFieldName));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapToIdFieldName)));
        subEqualsTo.setRightExpression(mapToIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(mapFromFieldTypeName);
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromFieldName)));
        equalsTo.setRightExpression(subSelect);
        if (mapFromIdValueExpressionList.size() > 0) {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(mapFromIdFieldName)));
            inExpression.setNot(true);
            inExpression.setRightItemsList(new ExpressionList(mapFromIdValueExpressionList));
            delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        } else {
            delete.setWhere(equalsTo);
        }
        return delete;
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

    protected Insert singleTypeScalarArgumentsToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                       GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(fieldDefinitionContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        insert.setUseDuplicate(true);
        insert.setDuplicateUpdateColumns(columnList);
        insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                         GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList = inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(fieldDefinitionContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        insert.setUseDuplicate(true);
        insert.setDuplicateUpdateColumns(columnList);
        insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                         GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                         GraphqlParser.ObjectValueContext objectValueContext) {

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList = inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(fieldDefinitionContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        insert.setUseDuplicate(true);
        insert.setDuplicateUpdateColumns(columnList);
        insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> singleTypeScalarArgumentsToColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        if (argumentContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(argumentContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldWithVariableContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(manager.getFieldTypeName(fieldDefinitionContext.type()));
        Table table = new Table(tableName);
        if (objectFieldContext.isPresent()) {
            return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(objectFieldContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarArgumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                      GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                        GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                        GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.get().value()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }
}
