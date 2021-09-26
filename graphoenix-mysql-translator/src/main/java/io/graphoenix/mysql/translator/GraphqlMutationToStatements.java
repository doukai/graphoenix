package io.graphoenix.mysql.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.common.utils.DocumentUtil;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
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
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(mutationTypeName -> manager.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText()));
        if (mutationFieldTypeDefinitionContext.isPresent()) {
            return argumentsToStatementStream(mutationFieldTypeDefinitionContext.get(), selectionContext.field().arguments());
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext) {
        return argumentsToStatementStream(fieldDefinitionContext, argumentsContext, 0, 0);
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext,
                                                           int level,
                                                           int index) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarArgumentsToInsert(fieldDefinitionContext, argumentsContext);
        statementList.add(insert);

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

        Optional<GraphqlParser.ArgumentContext> idArgumentContext = manager.getIDArgument(fieldDefinitionContext.type(), argumentsContext);
        if (idArgumentContext.isEmpty()) {
            idFieldName.ifPresent(name -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, name, level, index)));
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
                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
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
                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                if (mapWithTypeArgument.isPresent()) {
                                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                        statementList.add(insertMapWithType(
                                                mapWithTypeName.get(),
                                                mapWithFromFieldName.get(),
                                                mapWithToFieldName.get(),
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                        ));
                                    }
                                } else {
                                    statementList.addAll(updateMapFieldEachOther(
                                            fromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                            toFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
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

                                    if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                        Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());

                                        if (mapWithTypeArgument.isPresent()) {
                                            Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                            Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                            Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                            if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                                ));
                                            }
                                        } else {
                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
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
                            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                if (argumentContext.isPresent()) {
                                    Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                    List<Expression> updateObjectFieldExpressionList = new ArrayList<>();

                                    if (mapWithTypeArgument.isPresent()) {
                                        Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                        Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                        Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                                        if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                            int subIndex = 0;
                                            for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                                statementList.addAll(
                                                        singleTypeObjectValueWithVariableToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                inputObjectTypeDefinition.get(),
                                                                valueWithVariableContext.objectValueWithVariable(),
                                                                level + 1,
                                                                subIndex
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                                updateObjectFieldExpressionList.add(objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                ));
                                                subIndex++;
                                            }
                                            statementList.add(deleteMapToWithType(
                                                    mapWithTypeName.get(),
                                                    mapWithFromFieldName.get(),
                                                    mapWithToFieldName.get(),
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    updateObjectFieldExpressionList
                                            ));
                                        }
                                    } else {
                                        int subIndex = 0;
                                        for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                            statementList.addAll(
                                                    singleTypeObjectValueWithVariableToStatementStream(
                                                            subFieldDefinitionContext.get(),
                                                            inputObjectTypeDefinition.get(),
                                                            valueWithVariableContext.objectValueWithVariable(),
                                                            level + 1,
                                                            subIndex
                                                    ).collect(Collectors.toList()));

                                            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                            updateObjectFieldExpressionList.add(objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                            ));
                                            subIndex++;
                                        }

                                        statementList.add(deleteMapToField(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                updateObjectFieldExpressionList
                                        ));

                                    }
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                            List<Expression> updateObjectFieldExpressionList = new ArrayList<>();

                                            if (mapWithTypeArgument.isPresent()) {
                                                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                                                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                    int subIndex = 0;
                                                    for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                        statementList.addAll(
                                                                singleTypeObjectValueToStatementStream(
                                                                        subFieldDefinitionContext.get(),
                                                                        inputObjectTypeDefinition.get(),
                                                                        valueContext.objectValue(),
                                                                        level + 1,
                                                                        subIndex
                                                                ).collect(Collectors.toList()));

                                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                        updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                        statementList.add(insertMapWithType(
                                                                mapWithTypeName.get(),
                                                                mapWithFromFieldName.get(),
                                                                mapWithToFieldName.get(),
                                                                fromFieldDefinition.get().name().getText(),
                                                                fieldTypeName,
                                                                idFieldName.get(),
                                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                                toFieldDefinition.get().name().getText(),
                                                                subFieldTypeName,
                                                                subIdFieldName.get(),
                                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                        ));
                                                        subIndex++;
                                                    }

                                                    statementList.add(deleteMapToWithType(
                                                            mapWithTypeName.get(),
                                                            mapWithFromFieldName.get(),
                                                            mapWithToFieldName.get(),
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            updateObjectFieldExpressionList
                                                    ));
                                                }
                                            } else {
                                                int subIndex = 0;
                                                for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                    statementList.addAll(
                                                            singleTypeObjectValueToStatementStream(
                                                                    subFieldDefinitionContext.get(),
                                                                    inputObjectTypeDefinition.get(),
                                                                    valueContext.objectValue(),
                                                                    level + 1,
                                                                    subIndex
                                                            ).collect(Collectors.toList()));

                                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                    updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                    statementList.addAll(updateMapFieldEachOther(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                    ));
                                                    subIndex++;
                                                }
                                                statementList.add(deleteMapToField(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        updateObjectFieldExpressionList
                                                ));
                                            }
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);

                        if (argumentContext.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);

                            if (subFieldDefinitionContext.isPresent()) {
                                Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());

                                if (mapWithTypeArgument.isPresent()) {
                                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());

                                    if (fromFieldDefinition.isPresent() && idFieldName.isPresent() && mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                        statementList.add(deleteScalarMapToWithType(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                mapWithFromFieldName.get(),
                                                mapWithTypeName.get()
                                        ));

                                        for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeScalarInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        mapWithToFieldName.get(),
                                                        mapWithTypeName.get(),
                                                        mapWithToFieldName.get(),
                                                        valueWithVariableContext));

                                            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeEnumInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        mapWithToFieldName.get(),
                                                        mapWithTypeName.get(),
                                                        mapWithToFieldName.get(),
                                                        valueWithVariableContext));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (inputValueDefinitionContext.type().nonNullType() != null) {
                            if (inputValueDefinitionContext.defaultValue() != null) {

                                Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                                if (subFieldDefinitionContext.isPresent()) {
                                    Optional<GraphqlParser.ObjectTypeDefinitionContext> mapInObjectDefinitionContext = manager.getMapInObjectTypeDefinition(subFieldDefinitionContext.get());

                                    if (mapInObjectDefinitionContext.isPresent()) {
                                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(mapInObjectDefinitionContext.get().name().getText(), subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinition = manager.getFieldDefinitionContextByType(mapInObjectDefinitionContext.get().name().getText(), manager.getFieldTypeName(inputValueDefinitionContext.type()));
                                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

                                        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subFieldDefinition.isPresent()) {
                                            statementList.add(deleteScalarMapToWithType(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    mapInObjectDefinitionContext.get().name().getText()
                                            ));

                                            for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeScalarInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));

                                                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeEnumInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idArgumentContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                //TODO
                            }
                        }
                    }
                });
        return statementList.stream();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                   int level) {
        return singleTypeObjectValueWithVariableToStatementStream(fieldDefinitionContext, inputObjectTypeDefinition, objectValueWithVariableContext, level, 0);
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                   GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                   int level,
                                                                                   int index) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarInputValuesToInsert(fieldDefinitionContext, inputObjectTypeDefinition, objectValueWithVariableContext);
        statementList.add(insert);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariableContext = manager.getIDObjectFieldWithVariable(fieldDefinitionContext.type(), objectValueWithVariableContext);
        if (idObjectFieldWithVariableContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName, level, index)));
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
                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                        Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                        if (objectFieldWithVariableContext.isPresent()) {
                            statementList.addAll(
                                    singleTypeObjectValueWithVariableToStatementStream(
                                            subFieldDefinitionContext.get(),
                                            subInputObjectTypeDefinition.get(),
                                            objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable(),
                                            level + 1,
                                            index
                                    ).collect(Collectors.toList()));

                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                if (mapWithTypeArgument.isPresent()) {
                                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                        statementList.add(insertMapWithType(
                                                mapWithTypeName.get(),
                                                mapWithFromFieldName.get(),
                                                mapWithToFieldName.get(),
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                        ));
                                    }
                                } else {
                                    statementList.addAll(updateMapFieldEachOther(
                                            fromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                            toFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                    ));
                                }
                            }
                        } else {
                            if (inputValueDefinitionContext.type().nonNullType() != null) {
                                if (inputValueDefinitionContext.defaultValue() != null) {
                                    statementList.addAll(
                                            singleTypeObjectValueToStatementStream(
                                                    subFieldDefinitionContext.get(),
                                                    subInputObjectTypeDefinition.get(),
                                                    inputValueDefinitionContext.defaultValue().value().objectValue(),
                                                    level + 1,
                                                    index
                                            ).collect(Collectors.toList()));

                                    if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), inputValueDefinitionContext.defaultValue().value().objectValue());
                                        Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                        if (mapWithTypeArgument.isPresent()) {
                                            Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                            Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                            Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                            if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                                ));
                                            }
                                        } else {
                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
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


        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .forEach(inputValueDefinitionContext -> {

                    if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                        Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                        if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
                            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                if (objectFieldWithVariableContext.isPresent()) {
                                    Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                    List<Expression> updateObjectFieldExpressList = new ArrayList<>();

                                    if (mapWithTypeArgument.isPresent()) {
                                        Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                        Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                        Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                        if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {

                                            int subIndex = 0;
                                            for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                                statementList.addAll(
                                                        singleTypeObjectValueWithVariableToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                subInputObjectTypeDefinition.get(),
                                                                valueWithVariableContext.objectValueWithVariable(),
                                                                level + 1,
                                                                subIndex
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                                updateObjectFieldExpressList.add(objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                ));
                                                subIndex++;
                                            }
                                            statementList.add(deleteMapToWithType(
                                                    mapWithTypeName.get(),
                                                    mapWithFromFieldName.get(),
                                                    mapWithToFieldName.get(),
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    updateObjectFieldExpressList
                                            ));
                                        }
                                    } else {

                                        int subIndex = 0;
                                        for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                            statementList.addAll(
                                                    singleTypeObjectValueWithVariableToStatementStream(
                                                            subFieldDefinitionContext.get(),
                                                            subInputObjectTypeDefinition.get(),
                                                            valueWithVariableContext.objectValueWithVariable(),
                                                            level + 1,
                                                            subIndex
                                                    ).collect(Collectors.toList()));

                                            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectIdFieldWithVariableContext = manager.getIDObjectFieldWithVariable(subFieldDefinitionContext.get().type(), valueWithVariableContext.objectValueWithVariable());
                                            updateObjectFieldExpressList.add(objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                            ));
                                            subIndex++;
                                        }

                                        statementList.add(deleteMapToField(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                updateObjectFieldExpressList
                                        ));
                                    }
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                            List<Expression> updateObjectFieldExpressionList = new ArrayList<>();

                                            if (mapWithTypeArgument.isPresent()) {
                                                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {

                                                    int subIndex = 0;
                                                    for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                        statementList.addAll(
                                                                singleTypeObjectValueToStatementStream(
                                                                        subFieldDefinitionContext.get(),
                                                                        subInputObjectTypeDefinition.get(),
                                                                        valueContext.objectValue(),
                                                                        level + 1,
                                                                        subIndex
                                                                ).collect(Collectors.toList()));

                                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                        updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));
                                                        statementList.add(insertMapWithType(
                                                                mapWithTypeName.get(),
                                                                mapWithFromFieldName.get(),
                                                                mapWithToFieldName.get(),
                                                                fromFieldDefinition.get().name().getText(),
                                                                fieldTypeName,
                                                                idFieldName.get(),
                                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                                toFieldDefinition.get().name().getText(),
                                                                subFieldTypeName,
                                                                subIdFieldName.get(),
                                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                        ));
                                                        subIndex++;
                                                    }
                                                    statementList.add(deleteMapToWithType(
                                                            mapWithTypeName.get(),
                                                            mapWithFromFieldName.get(),
                                                            mapWithToFieldName.get(),
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            updateObjectFieldExpressionList
                                                    ));
                                                }
                                            } else {
                                                int subIndex = 0;
                                                for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {

                                                    statementList.addAll(
                                                            singleTypeObjectValueToStatementStream(
                                                                    subFieldDefinitionContext.get(),
                                                                    subInputObjectTypeDefinition.get(),
                                                                    valueContext.objectValue(),
                                                                    level + 1,
                                                                    subIndex
                                                            ).collect(Collectors.toList()));

                                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                    updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                    statementList.addAll(updateMapFieldEachOther(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                    ));
                                                    subIndex++;
                                                }
                                                inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {
                                                });

                                                statementList.add(deleteMapToField(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        updateObjectFieldExpressionList
                                                ));
                                            }
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
                        if (objectFieldWithVariableContext.isPresent()) {

                            Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                            if (subFieldDefinitionContext.isPresent()) {
                                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapInObjectDefinitionContext = manager.getMapInObjectTypeDefinition(subFieldDefinitionContext.get());

                                if (mapInObjectDefinitionContext.isPresent()) {
                                    Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                    Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(mapInObjectDefinitionContext.get().name().getText(), subFieldDefinitionContext.get());
                                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinition = manager.getFieldDefinitionContextByType(mapInObjectDefinitionContext.get().name().getText(), manager.getFieldTypeName(inputValueDefinitionContext.type()));
                                    Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

                                    if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subFieldDefinition.isPresent()) {
                                        statementList.add(deleteScalarMapToWithType(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                mapInObjectDefinitionContext.get().name().getText()
                                        ));

                                        for (GraphqlParser.ValueWithVariableContext valueWithVariableContext : objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable()) {
                                            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeScalarInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        mapInObjectDefinitionContext.get().name().getText(),
                                                        subFieldDefinition.get().name().getText(),
                                                        valueWithVariableContext));

                                            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeEnumInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        mapInObjectDefinitionContext.get().name().getText(),
                                                        subFieldDefinition.get().name().getText(),
                                                        valueWithVariableContext));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (inputValueDefinitionContext.type().nonNullType() != null) {
                            if (inputValueDefinitionContext.defaultValue() != null) {

                                Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                                if (subFieldDefinitionContext.isPresent()) {
                                    Optional<GraphqlParser.ObjectTypeDefinitionContext> mapInObjectDefinitionContext = manager.getMapInObjectTypeDefinition(subFieldDefinitionContext.get());

                                    if (mapInObjectDefinitionContext.isPresent()) {
                                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(mapInObjectDefinitionContext.get().name().getText(), subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinition = manager.getFieldDefinitionContextByType(mapInObjectDefinitionContext.get().name().getText(), manager.getFieldTypeName(inputValueDefinitionContext.type()));
                                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

                                        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subFieldDefinition.isPresent()) {
                                            statementList.add(deleteScalarMapToWithType(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    mapInObjectDefinitionContext.get().name().getText()
                                            ));

                                            for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeScalarInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));

                                                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeEnumInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldWithVariableContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                //TODO
                            }
                        }
                    }
                });
        return statementList.stream();
    }

    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                       GraphqlParser.ObjectValueContext objectValueContext,
                                                                       int level) {
        return singleTypeObjectValueToStatementStream(fieldDefinitionContext, inputObjectTypeDefinition, objectValueContext, level, 0);
    }

    protected Stream<Statement> singleTypeObjectValueToStatementStream(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinition,
                                                                       GraphqlParser.ObjectValueContext objectValueContext,
                                                                       int level,
                                                                       int index) {

        List<Statement> statementList = new ArrayList<>();
        Statement insert = singleTypeScalarInputValuesToInsert(fieldDefinitionContext, inputObjectTypeDefinition, objectValueContext);
        statementList.add(insert);
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());

        Optional<GraphqlParser.ObjectFieldContext> idObjectFieldContext = manager.getIDObjectField(fieldDefinitionContext.type(), objectValueContext);
        if (idObjectFieldContext.isEmpty()) {
            manager.getObjectTypeIDFieldName(fieldTypeName).ifPresent(idFieldName -> statementList.add(DB_VALUE_UTIL.createInsertIdSetStatement(fieldTypeName, idFieldName, level, index)));
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
                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
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

                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                if (mapWithTypeArgument.isPresent()) {
                                    Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                    Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                    Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                    if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                        statementList.add(insertMapWithType(
                                                mapWithTypeName.get(),
                                                mapWithFromFieldName.get(),
                                                mapWithToFieldName.get(),
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                        ));
                                    }
                                } else {
                                    statementList.addAll(updateMapFieldEachOther(
                                            fromFieldDefinition.get().name().getText(),
                                            fieldTypeName,
                                            idFieldName.get(),
                                            idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                            toFieldDefinition.get().name().getText(),
                                            subFieldTypeName,
                                            subIdFieldName.get(),
                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                    ));
                                }
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
                                    if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {
                                        Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                        if (mapWithTypeArgument.isPresent()) {
                                            Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                            Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                            Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                            if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {
                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
                                                ));
                                            }
                                        } else {
                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, index))
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

        inputObjectTypeDefinition.inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                .forEach(inputValueDefinitionContext -> {
                    if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {

                        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                        Optional<GraphqlParser.InputObjectTypeDefinitionContext> subInputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

                        if (subFieldDefinitionContext.isPresent() && subInputObjectTypeDefinition.isPresent()) {
                            Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                            Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(subFieldDefinitionContext.get());
                            Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);
                            String subFieldTypeName = manager.getFieldTypeName(subFieldDefinitionContext.get().type());
                            Optional<String> subIdFieldName = manager.getObjectTypeIDFieldName(subFieldTypeName);

                            if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subIdFieldName.isPresent()) {

                                if (objectFieldContext.isPresent()) {
                                    Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                    List<Expression> updateObjectFieldExpressionList = new ArrayList<>();

                                    if (mapWithTypeArgument.isPresent()) {
                                        Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                        Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                        Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                        if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {

                                            int subIndex = 0;
                                            for (GraphqlParser.ValueContext valueContext : objectFieldContext.get().value().arrayValue().value()) {
                                                statementList.addAll(
                                                        singleTypeObjectValueToStatementStream(
                                                                subFieldDefinitionContext.get(),
                                                                subInputObjectTypeDefinition.get(),
                                                                valueContext.objectValue(),
                                                                level + 1,
                                                                subIndex
                                                        ).collect(Collectors.toList()));

                                                Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                statementList.add(insertMapWithType(
                                                        mapWithTypeName.get(),
                                                        mapWithFromFieldName.get(),
                                                        mapWithToFieldName.get(),
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                ));
                                                subIndex++;
                                            }
                                            statementList.add(deleteMapToWithType(
                                                    mapWithTypeName.get(),
                                                    mapWithFromFieldName.get(),
                                                    mapWithToFieldName.get(),
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    updateObjectFieldExpressionList
                                            ));
                                        }
                                    } else {
                                        int subIndex = 0;
                                        for (GraphqlParser.ValueContext valueContext : objectFieldContext.get().value().arrayValue().value()) {

                                            statementList.addAll(
                                                    singleTypeObjectValueToStatementStream(
                                                            subFieldDefinitionContext.get(),
                                                            subInputObjectTypeDefinition.get(),
                                                            valueContext.objectValue(),
                                                            level + 1,
                                                            subIndex
                                                    ).collect(Collectors.toList()));

                                            Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                            updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                            statementList.addAll(updateMapFieldEachOther(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    subFieldTypeName,
                                                    subIdFieldName.get(),
                                                    objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                            ));
                                            subIndex++;
                                        }
                                        objectFieldContext.get().value().arrayValue().value().forEach(valueContext -> {
                                        });

                                        statementList.add(deleteMapToField(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                subFieldTypeName,
                                                subIdFieldName.get(),
                                                updateObjectFieldExpressionList
                                        ));
                                    }
                                } else {
                                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                                        if (inputValueDefinitionContext.defaultValue() != null) {
                                            Optional<GraphqlParser.ArgumentContext> mapWithTypeArgument = manager.getMapWithTypeArgument(subFieldDefinitionContext.get());
                                            List<Expression> updateObjectFieldExpressionList = new ArrayList<>();

                                            if (mapWithTypeArgument.isPresent()) {
                                                Optional<String> mapWithTypeName = manager.getMapWithTypeName(mapWithTypeArgument.get());
                                                Optional<String> mapWithFromFieldName = manager.getMapWithTypeFromFieldName(mapWithTypeArgument.get());
                                                Optional<String> mapWithToFieldName = manager.getMapWithTypeToFieldName(mapWithTypeArgument.get());
                                                if (mapWithTypeName.isPresent() && mapWithFromFieldName.isPresent() && mapWithToFieldName.isPresent()) {

                                                    int subIndex = 0;
                                                    for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {

                                                        statementList.addAll(
                                                                singleTypeObjectValueToStatementStream(
                                                                        subFieldDefinitionContext.get(),
                                                                        subInputObjectTypeDefinition.get(),
                                                                        valueContext.objectValue(),
                                                                        level + 1,
                                                                        subIndex
                                                                ).collect(Collectors.toList()));

                                                        Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                        updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));
                                                        statementList.add(insertMapWithType(
                                                                mapWithTypeName.get(),
                                                                mapWithFromFieldName.get(),
                                                                mapWithToFieldName.get(),
                                                                fromFieldDefinition.get().name().getText(),
                                                                fieldTypeName,
                                                                idFieldName.get(),
                                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                                toFieldDefinition.get().name().getText(),
                                                                subFieldTypeName,
                                                                subIdFieldName.get(),
                                                                objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                        ));
                                                        subIndex++;
                                                    }

                                                    statementList.add(deleteMapToWithType(
                                                            mapWithTypeName.get(),
                                                            mapWithFromFieldName.get(),
                                                            mapWithToFieldName.get(),
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            updateObjectFieldExpressionList
                                                    ));
                                                }
                                            } else {
                                                int subIndex = 0;
                                                for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {

                                                    statementList.addAll(
                                                            singleTypeObjectValueToStatementStream(
                                                                    subFieldDefinitionContext.get(),
                                                                    subInputObjectTypeDefinition.get(),
                                                                    valueContext.objectValue(),
                                                                    level + 1,
                                                                    subIndex
                                                            ).collect(Collectors.toList()));

                                                    Optional<GraphqlParser.ObjectFieldContext> objectIdFieldContext = manager.getIDObjectField(subFieldDefinitionContext.get().type(), valueContext.objectValue());
                                                    updateObjectFieldExpressionList.add(objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex)));

                                                    statementList.addAll(updateMapFieldEachOther(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            subFieldTypeName,
                                                            subIdFieldName.get(),
                                                            objectIdFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(subFieldTypeName, subIdFieldName.get(), level + 1, subIndex))
                                                    ));
                                                    subIndex++;
                                                }
                                                inputValueDefinitionContext.defaultValue().value().arrayValue().value().forEach(valueContext -> {
                                                });

                                                statementList.add(deleteMapToField(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        subFieldTypeName,
                                                        subIdFieldName.get(),
                                                        updateObjectFieldExpressionList
                                                ));
                                            }
                                        } else {
                                            //TODO
                                        }
                                    }
                                }
                            }
                        }
                    } else if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
                        if (objectFieldContext.isPresent()) {

                            Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                            if (subFieldDefinitionContext.isPresent()) {
                                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapInObjectDefinitionContext = manager.getMapInObjectTypeDefinition(subFieldDefinitionContext.get());

                                if (mapInObjectDefinitionContext.isPresent()) {
                                    Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                    Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(mapInObjectDefinitionContext.get().name().getText(), subFieldDefinitionContext.get());
                                    Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinition = manager.getFieldDefinitionContextByType(mapInObjectDefinitionContext.get().name().getText(), manager.getFieldTypeName(inputValueDefinitionContext.type()));
                                    Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

                                    if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subFieldDefinition.isPresent()) {
                                        statementList.add(deleteScalarMapToWithType(
                                                fromFieldDefinition.get().name().getText(),
                                                fieldTypeName,
                                                idFieldName.get(),
                                                idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                toFieldDefinition.get().name().getText(),
                                                mapInObjectDefinitionContext.get().name().getText()
                                        ));

                                        for (GraphqlParser.ValueContext valueContext : objectFieldContext.get().value().arrayValue().value()) {
                                            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeScalarInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        mapInObjectDefinitionContext.get().name().getText(),
                                                        subFieldDefinition.get().name().getText(),
                                                        valueContext));

                                            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                statementList.add(singleTypeEnumInputValueToInsert(
                                                        fromFieldDefinition.get().name().getText(),
                                                        fieldTypeName,
                                                        idFieldName.get(),
                                                        idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                        toFieldDefinition.get().name().getText(),
                                                        mapInObjectDefinitionContext.get().name().getText(),
                                                        subFieldDefinition.get().name().getText(),
                                                        valueContext));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (inputValueDefinitionContext.type().nonNullType() != null) {
                            if (inputValueDefinitionContext.defaultValue() != null) {

                                Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(fieldDefinitionContext.type(), inputValueDefinitionContext);
                                if (subFieldDefinitionContext.isPresent()) {
                                    Optional<GraphqlParser.ObjectTypeDefinitionContext> mapInObjectDefinitionContext = manager.getMapInObjectTypeDefinition(subFieldDefinitionContext.get());

                                    if (mapInObjectDefinitionContext.isPresent()) {
                                        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = manager.getMapFromFieldDefinition(fieldTypeName, subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = manager.getMapToFieldDefinition(mapInObjectDefinitionContext.get().name().getText(), subFieldDefinitionContext.get());
                                        Optional<GraphqlParser.FieldDefinitionContext> subFieldDefinition = manager.getFieldDefinitionContextByType(mapInObjectDefinitionContext.get().name().getText(), manager.getFieldTypeName(inputValueDefinitionContext.type()));
                                        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(fieldTypeName);

                                        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent() && idFieldName.isPresent() && subFieldDefinition.isPresent()) {
                                            statementList.add(deleteScalarMapToWithType(
                                                    fromFieldDefinition.get().name().getText(),
                                                    fieldTypeName,
                                                    idFieldName.get(),
                                                    idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                    toFieldDefinition.get().name().getText(),
                                                    mapInObjectDefinitionContext.get().name().getText()
                                            ));

                                            for (GraphqlParser.ValueContext valueContext : inputValueDefinitionContext.defaultValue().value().arrayValue().value()) {
                                                if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeScalarInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));

                                                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                                                    statementList.add(singleTypeEnumInputValueToInsert(
                                                            fromFieldDefinition.get().name().getText(),
                                                            fieldTypeName,
                                                            idFieldName.get(),
                                                            idObjectFieldContext.map(this::createIdValueExpression).orElse(DB_VALUE_UTIL.createInsertIdUserVariable(fieldTypeName, idFieldName.get(), level, index)),
                                                            toFieldDefinition.get().name().getText(),
                                                            mapInObjectDefinitionContext.get().name().getText(),
                                                            subFieldDefinition.get().name().getText(),
                                                            valueContext));
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                //TODO
                            }
                        }
                    }
                });

        return statementList.stream();
    }

    protected List<Update> updateMapFieldEachOther(String fromFieldName,
                                                   String fromTypeName,
                                                   String fromFieldIdName,
                                                   Expression fromFieldIdValueExpression,
                                                   String toFieldName,
                                                   String toTypeName,
                                                   String toFieldIdName,
                                                   Expression toFieldIdValueExpression) {
        return Arrays.asList(
                updateMapField(fromFieldName, fromTypeName, fromFieldIdName, fromFieldIdValueExpression, toFieldName, toTypeName, toFieldIdName, toFieldIdValueExpression),
                updateMapField(toFieldName, toTypeName, toFieldIdName, toFieldIdValueExpression, fromFieldName, fromTypeName, fromFieldIdName, fromFieldIdValueExpression)
        );
    }

    protected Update updateMapField(String fromFieldName,
                                    String fromTypeName,
                                    String fromFieldIdName,
                                    Expression fromFieldIdValueExpression,
                                    String toFieldName,
                                    String toTypeName,
                                    String toFieldIdName,
                                    Expression toFieldIdValueExpression) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)));
        selectExpressionItem.setAlias(new Alias(toFieldName));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldIdName)));
        subEqualsTo.setRightExpression(toFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Update update = new Update();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table table = new Table(tableName);
        update.setTable(table);
        Column updateColumn = new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName));
        update.setColumns(Collections.singletonList(updateColumn));
        update.setExpressions(Collections.singletonList(subSelect));
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        equalsTo.setRightExpression(fromFieldIdValueExpression);
        IsNullExpression isNullExpression = new IsNullExpression();
        isNullExpression.setLeftExpression(updateColumn);
        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(isNullExpression, equalsTo));
        update.setWhere(multiAndExpression);
        return update;
    }

    protected Delete deleteMapToField(String fromFieldName,
                                      String fromTypeName,
                                      String fromFieldIdName,
                                      Expression fromFieldIdValueExpression,
                                      String toFieldName,
                                      String toTypeName,
                                      String toFieldIdName,
                                      List<Expression> toFieldIdValueExpressionList) {

        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName);
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)));
        equalsTo.setRightExpression(subSelect);
        if (toFieldIdValueExpressionList.size() > 0) {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldIdName)));
            inExpression.setNot(true);
            inExpression.setRightItemsList(new ExpressionList(toFieldIdValueExpressionList));
            delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        } else {
            delete.setWhere(equalsTo);
        }
        return delete;
    }

    protected Insert insertMapWithType(String withTypeName,
                                       String withTypeFromFieldName,
                                       String withTypeToFieldName,
                                       String fromFieldName,
                                       String fromTypeName,
                                       String fromFieldIdName,
                                       Expression fromFieldIdValueExpression,
                                       String toFieldName,
                                       String toTypeName,
                                       String toFieldIdName,
                                       Expression toFieldIdValueExpression) {
        Insert insert = new Insert();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(withTypeName);
        Table table = new Table(tableName);
        insert.setTable(table);

        Select select = new Select();
        PlainSelect plainSelect = new PlainSelect();
        plainSelect.setFromItem(table);

        String fromTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table fromTable = new Table(fromTableName);
        Join joinFromTable = new Join();
        EqualsTo fromTableEqualsTo = new EqualsTo();
        fromTableEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeFromFieldName)));
        fromTableEqualsTo.setRightExpression(new Column(fromTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        joinFromTable.setLeft(true);
        joinFromTable.setRightItem(fromTable);
        joinFromTable.setOnExpression(fromTableEqualsTo);

        String toTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName);
        Table toTable = new Table(toTableName);
        Join joinToTable = new Join();
        EqualsTo toTableEqualsTo = new EqualsTo();
        toTableEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeToFieldName)));
        toTableEqualsTo.setRightExpression(new Column(toTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)));
        joinToTable.setLeft(true);
        joinToTable.setRightItem(toTable);
        joinToTable.setOnExpression(toTableEqualsTo);

        plainSelect.setJoins(Arrays.asList(joinFromTable, joinToTable));

        SelectExpressionItem fromColumnExpression = new SelectExpressionItem();
        fromColumnExpression.setExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeFromFieldName)));
        SelectExpressionItem toColumnExpression = new SelectExpressionItem();
        toColumnExpression.setExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeToFieldName)));

        plainSelect.setSelectItems(Arrays.asList(fromColumnExpression, toColumnExpression));

        EqualsTo fromIdEqualsTo = new EqualsTo();
        fromIdEqualsTo.setLeftExpression(new Column(fromTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        fromIdEqualsTo.setRightExpression(fromFieldIdValueExpression);

        EqualsTo toIdEqualsTo = new EqualsTo();
        toIdEqualsTo.setLeftExpression(new Column(toTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldIdName)));
        toIdEqualsTo.setRightExpression(toFieldIdValueExpression);

        MultiAndExpression multiAndExpression = new MultiAndExpression(Arrays.asList(fromIdEqualsTo, toIdEqualsTo));
        plainSelect.setWhere(multiAndExpression);

        select.setSelectBody(plainSelect);

        insert.setColumns(Arrays.asList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeFromFieldName)), new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeToFieldName))));
        insert.setSelect(select);

        return insert;
    }

    protected Delete deleteMapToWithType(String withTypeName,
                                         String withTypeFromFieldName,
                                         String withTypeToFieldName,
                                         String fromFieldName,
                                         String fromTypeName,
                                         String fromFieldIdName,
                                         Expression fromFieldIdValueExpression,
                                         String toFieldName,
                                         String toTypeName,
                                         String toFieldIdName,
                                         List<Expression> toFieldIdValueExpressionList) {
        Delete delete = new Delete();
        String tableName = DB_NAME_UTIL.graphqlTypeNameToTableName(withTypeName);
        Table table = new Table(tableName);
        delete.setTable(table);

        String fromTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table fromTable = new Table(fromTableName);
        Join joinFromTable = new Join();
        EqualsTo fromTableEqualsTo = new EqualsTo();
        fromTableEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeFromFieldName)));
        fromTableEqualsTo.setRightExpression(new Column(fromTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        joinFromTable.setLeft(true);
        joinFromTable.setRightItem(fromTable);
        joinFromTable.setOnExpression(fromTableEqualsTo);

        String toTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName);
        Table toTable = new Table(toTableName);
        Join joinToTable = new Join();
        EqualsTo toTableEqualsTo = new EqualsTo();
        toTableEqualsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(withTypeToFieldName)));
        toTableEqualsTo.setRightExpression(new Column(toTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)));
        joinToTable.setLeft(true);
        joinToTable.setRightItem(toTable);
        joinToTable.setOnExpression(toTableEqualsTo);

        delete.setJoins(Arrays.asList(joinFromTable, joinToTable));

        EqualsTo fromIdEqualsTo = new EqualsTo();
        fromIdEqualsTo.setLeftExpression(new Column(fromTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        fromIdEqualsTo.setRightExpression(fromFieldIdValueExpression);

        if (toFieldIdValueExpressionList.size() > 0) {
            InExpression toIdNotinExpression = new InExpression();
            toIdNotinExpression.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldIdName)));
            toIdNotinExpression.setNot(true);
            toIdNotinExpression.setRightItemsList(new ExpressionList(toFieldIdValueExpressionList));
            delete.setWhere(new MultiAndExpression(Arrays.asList(fromIdEqualsTo, toIdNotinExpression)));
        } else {
            delete.setWhere(fromIdEqualsTo);
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
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(fieldDefinitionContext, inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        if (columnList.size() > 0) {
            insert.setUseDuplicate(true);
            insert.setDuplicateUpdateColumns(columnList);
            insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        }
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
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(fieldDefinitionContext, inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        if (columnList.size() > 0) {
            insert.setUseDuplicate(true);
            insert.setDuplicateUpdateColumns(columnList);
            insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        }
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
                .filter(inputValueDefinitionContext -> manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))).collect(Collectors.toList());

        Insert insert = new Insert();
        List<Column> columnList = inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(fieldDefinitionContext, inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        ExpressionList expressionList = new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setColumns(columnList);
        insert.setItemsList(expressionList);
        if (columnList.size() > 0) {
            insert.setUseDuplicate(true);
            insert.setDuplicateUpdateColumns(columnList);
            insert.setDuplicateUpdateExpressionList(expressionList.getExpressions());
        }
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
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarArgumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                      GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable()));
            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(new StringValue(argumentContext.get().valueWithVariable().enumValue().getText()));
            }
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    System.out.println(inputValueDefinitionContext.getText());
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                        GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(DB_VALUE_UTIL.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.get().valueWithVariable()));
            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(new StringValue(objectFieldWithVariableContext.get().valueWithVariable().enumValue().getText()));
            }
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    System.out.println(objectValueWithVariableContext.getText());
                }
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                        GraphqlParser.ObjectValueContext objectValueContext) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            if (manager.isScaLar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(objectFieldContext.get().value()));
            } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                return Optional.of(new StringValue(objectFieldContext.get().value().enumValue().getText()));
            }
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(DB_VALUE_UTIL.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    System.out.println(objectValueContext.getText());
                }
            }
        }
        return Optional.empty();
    }

    protected Insert singleTypeScalarInputValueToInsert(String fromFieldName,
                                                        String fromTypeName,
                                                        String fromFieldIdName,
                                                        Expression fromFieldIdValueExpression,
                                                        String toFieldName,
                                                        String toTypeName,
                                                        String fieldName,
                                                        GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Insert insert = new Insert();
        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName));
        insert.setColumns(Arrays.asList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)), new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldName))));
        insert.setItemsList(new ExpressionList(Arrays.asList(subSelect, DB_VALUE_UTIL.scalarValueWithVariableToDBValue(valueWithVariableContext))));
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeEnumInputValueToInsert(String fromFieldName,
                                                      String fromTypeName,
                                                      String fromFieldIdName,
                                                      Expression fromFieldIdValueExpression,
                                                      String toFieldName,
                                                      String toTypeName,
                                                      String fieldName,
                                                      GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Insert insert = new Insert();
        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName));
        insert.setColumns(Arrays.asList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)), new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldName))));
        insert.setItemsList(new ExpressionList(Arrays.asList(subSelect, new StringValue(valueWithVariableContext.enumValue().getText()))));
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeScalarInputValueToInsert(String fromFieldName,
                                                        String fromTypeName,
                                                        String fromFieldIdName,
                                                        Expression fromFieldIdValueExpression,
                                                        String toFieldName,
                                                        String toTypeName,
                                                        String fieldName,
                                                        GraphqlParser.ValueContext valueContext) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Insert insert = new Insert();
        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName));
        insert.setColumns(Arrays.asList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)), new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldName))));
        insert.setItemsList(new ExpressionList(Arrays.asList(subSelect, DB_VALUE_UTIL.scalarValueToDBValue(valueContext))));
        insert.setTable(table);
        return insert;
    }

    protected Insert singleTypeEnumInputValueToInsert(String fromFieldName,
                                                      String fromTypeName,
                                                      String fromFieldIdName,
                                                      Expression fromFieldIdValueExpression,
                                                      String toFieldName,
                                                      String toTypeName,
                                                      String fieldName,
                                                      GraphqlParser.ValueContext valueContext) {
        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Insert insert = new Insert();
        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName));
        insert.setColumns(Arrays.asList(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)), new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(fieldName))));
        insert.setItemsList(new ExpressionList(Arrays.asList(subSelect, new StringValue(valueContext.enumValue().getText()))));
        insert.setTable(table);
        return insert;
    }


    protected Delete deleteScalarMapToWithType(String fromFieldName,
                                               String fromTypeName,
                                               String fromFieldIdName,
                                               Expression fromFieldIdValueExpression,
                                               String toFieldName,
                                               String toTypeName) {

        SubSelect subSelect = new SubSelect();
        PlainSelect subBody = new PlainSelect();
        String subTableName = DB_NAME_UTIL.graphqlTypeNameToTableName(fromTypeName);
        Table subTable = new Table(subTableName);
        SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
        selectExpressionItem.setExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldName)));
        subBody.setSelectItems(Collections.singletonList(selectExpressionItem));
        EqualsTo subEqualsTo = new EqualsTo();
        subEqualsTo.setLeftExpression(new Column(subTable, DB_NAME_UTIL.graphqlFieldNameToColumnName(fromFieldIdName)));
        subEqualsTo.setRightExpression(fromFieldIdValueExpression);
        subBody.setWhere(subEqualsTo);
        subBody.setFromItem(subTable);
        subSelect.setSelectBody(subBody);

        Delete delete = new Delete();
        Table table = new Table(DB_NAME_UTIL.graphqlTypeNameToTableName(toTypeName));
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DB_NAME_UTIL.graphqlFieldNameToColumnName(toFieldName)));
        equalsTo.setRightExpression(subSelect);
        delete.setWhere(equalsTo);
        return delete;
    }
}
