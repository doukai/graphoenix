package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrRegister register;

    public GraphqlMutationToStatements(GraphqlAntlrRegister register) {
        this.register = register;
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
        if (operationDefinitionContext.name() != null) {
            //TODO
        }
        if (operationDefinitionContext.variableDefinitions() != null) {
            //TODO
        }
        if (operationDefinitionContext.directives() != null) {
            //TODO
        }
        if (operationDefinitionContext.operationType() != null && operationDefinitionContext.operationType().getText().equals("mutation")) {
            return operationDefinitionContext.selectionSet().selection().stream().flatMap(this::selectionToStatementStream);
        }
        return Stream.empty();
    }

    protected Stream<Statement> selectionToStatementStream(GraphqlParser.SelectionContext selectionContext) {
        String mutationTypeName = register.getMutationTypeName();
        Optional<GraphqlParser.TypeContext> mutationFieldTypeContext = register.getObjectFieldTypeContext(mutationTypeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> mutationFieldTypeDefinitionContext = register.getObjectFieldDefinitionContext(mutationTypeName, selectionContext.field().name().getText());

        if (mutationFieldTypeContext.isPresent()) {
            if (mutationFieldTypeDefinitionContext.isPresent()) {
                return argumentsToStatementStream(mutationFieldTypeContext.get(), mutationFieldTypeDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> register.isInnerScalar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        singleTypeScalarArgumentsToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, argumentsContext),
                        argumentsDefinitionContext.inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                                .filter(inputValueDefinitionContext -> register.isInputObject(inputValueDefinitionContext.type().getText()))
                                .flatMap(inputValueDefinitionContext -> singleTypeArgumentToStatementStream(typeContext, inputValueDefinitionContext, argumentsContext))
                ),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext -> register.fieldTypeIsList(inputValueDefinitionContext.type()))
                        .flatMap(inputValueDefinitionContext -> listTypeArgumentToStatement(typeContext, inputValueDefinitionContext, argumentsContext))
        );
    }

    protected Stream<Statement> singleTypeArgumentToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(typeContext)).objectTypeDefinition();
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().objectValueWithVariable() != null) {
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ArgumentContext> idArgument = getIdArgument(typeContext, argumentsContext);
                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, argumentContext.get().valueWithVariable().objectValueWithVariable());
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(typeContext)).objectTypeDefinition();
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
                    return singleTypeObjectValueWithVariableToStatementStream(typeContext, idObjectFieldWithVariable.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> singleTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentTypeIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, singleTypeScalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(parentTypeRelationUpdate(parentTypeContext, parentTypeIdValueWithVariableContext, typeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
        );
    }

    protected Stream<Statement> listTypeArgumentToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(typeContext)).objectTypeDefinition();
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
                if (fieldDefinitionContext.isPresent()) {

                    Optional<GraphqlParser.ArgumentContext> idArgument = getIdArgument(typeContext, argumentsContext);
                    return Stream.concat(argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                            Stream.of(listTypeFieldDelete(typeContext, idArgument.map(GraphqlParser.ArgumentContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
                                    argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> getIdObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
                                            .filter(Optional::isPresent)
                                            .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.get().valueWithVariable())
                                            .collect(Collectors.toList())))
                    );
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(typeContext)).objectTypeDefinition();
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
                    return Stream.concat(objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                    .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                    .flatMap(valueWithVariableContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                            Stream.of(listTypeFieldDelete(typeContext, idField.map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable).orElse(null), fieldDefinitionContext.get().type(),
                                    objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> getIdObjectFieldWithVariable(typeContext, valueWithVariableContext.objectValueWithVariable()))
                                            .filter(Optional::isPresent)
                                            .map(updateObjectFieldWithVariableContext -> updateObjectFieldWithVariableContext.get().valueWithVariable())
                                            .collect(Collectors.toList())))
                    );
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeObjectValueWithVariableToStatementStream(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentTypeIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));
        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                .filter(fieldInputValueDefinitionContext -> register.isInnerScalar(fieldInputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.concat(singleTypeScalarInputValuesToStatementStream(typeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(fieldTypeRelationUpdate(parentTypeContext, parentTypeIdValueWithVariableContext, typeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(fieldInputValueDefinitionContext -> !register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                                .filter(fieldInputValueDefinitionContext -> register.isInputObject(fieldInputValueDefinitionContext.type().getText()))
                                .flatMap(fieldInputValueDefinitionContext -> singleTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(fieldInputValueDefinitionContext -> register.fieldTypeIsList(fieldInputValueDefinitionContext.type()))
                        .flatMap(fieldInputValueDefinitionContext -> listTypeObjectValueWithVariableToStatementStream(typeContext, fieldInputValueDefinitionContext, objectValueWithVariableContext))
        );
    }

    protected Stream<Statement> singleTypeScalarArgumentsToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> idField = getIdArgument(typeContext, argumentsContext);
        return idField.<Stream<Statement>>map(argumentContext -> Stream.of(singleTypeScalarArgumentsToUpdate(typeContext, argumentContext, inputValueDefinitionContextList, argumentsContext))).orElseGet(() -> Stream.of(singleTypeScalarArgumentsToInsert(typeContext, inputValueDefinitionContextList, argumentsContext), createInsertIdSetStatement(typeContext)));
    }

    protected Update singleTypeScalarArgumentsToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert singleTypeScalarArgumentsToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToColumn(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarArgumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> singleTypeScalarArgumentsToColumn(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return Optional.of(new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarArgumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return Optional.of(register.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Stream<Statement> singleTypeScalarInputValuesToStatementStream(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        return idField.<Stream<Statement>>map(objectFieldWithVariableContext -> Stream.of(singleTypeScalarInputValuesToUpdate(typeContext, objectFieldWithVariableContext, inputValueDefinitionContextList, objectValueWithVariableContext))).orElseGet(() -> Stream.of(singleTypeScalarInputValuesToInsert(typeContext, inputValueDefinitionContextList, objectValueWithVariableContext), createInsertIdSetStatement(typeContext)));
    }

    protected Update singleTypeScalarInputValuesToUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert singleTypeScalarInputValuesToInsert(GraphqlParser.TypeContext typeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToColumn(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> singleTypeScalarInputValuesToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> singleTypeScalarInputValuesToColumn(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(objectFieldWithVariableContext.get().name().getText())));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return Optional.of(new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText())));
            } else {
                //todo
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> singleTypeScalarInputValuesToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return Optional.of(register.scalarValueWithVariableToDBValue(objectFieldWithVariableContext.get().valueWithVariable()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return Optional.of(register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value()));
                } else {
                    //todo
                }
            }
        }
        return Optional.empty();
    }

    protected Update parentTypeRelationUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(parentTypeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(parentTypeContext), register.getFieldTypeName(typeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        if (fieldIdField.isPresent()) {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable())));
        } else {
            Function function = new Function();
            function.setName("LAST_INSERT_ID");
            update.setExpressions(Collections.singletonList(function));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (parentIdValueWithVariableContext == null) {
            equalsTo.setRightExpression(createInsertIdUserVariable(parentTypeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Update fieldTypeRelationUpdate(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext parentIdValueWithVariableContext, GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(typeContext), register.getFieldTypeName(parentTypeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = getIdObjectFieldWithVariable(typeContext, objectValueWithVariableContext);
        if (parentIdValueWithVariableContext == null) {
            update.setExpressions(Collections.singletonList(createInsertIdUserVariable(typeContext)));
        } else {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(parentIdValueWithVariableContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (fieldIdField.isPresent()) {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable()));
        } else {
            equalsTo.setRightExpression(createInsertIdUserVariable(typeContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Delete listTypeFieldDelete(GraphqlParser.TypeContext parentTypeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext typeContext, List<GraphqlParser.ValueWithVariableContext> idValueWithVariableContextList) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(parentTypeContext)))));
        if (idValueWithVariableContext == null) {
            equalsTo.setRightExpression(createInsertIdUserVariable(typeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idValueWithVariableContext));
        }
        InExpression inExpression = new InExpression();
        inExpression.setNot(true);
        inExpression.setRightItemsList(new ExpressionList(idValueWithVariableContextList.stream().map(register::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        return delete;
    }

    protected Optional<GraphqlParser.ArgumentContext> getIdArgument(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        String typeIdFieldName = register.getTypeIdFieldName(register.getFieldTypeName(fieldTypeContext));
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Optional<GraphqlParser.ObjectFieldWithVariableContext> getIdObjectFieldWithVariable(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String typeIdFieldName = register.getTypeIdFieldName(register.getFieldTypeName(fieldTypeContext));
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(fieldWithVariableContext -> fieldWithVariableContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected String getIdVariableName(GraphqlParser.TypeContext typeContext) {
        String typeName = register.getFieldTypeName(typeContext);
        return DBNameConverter.INSTANCE.graphqlFieldNameToVariableName(typeName, register.getTypeIdFieldName(typeName));
    }

    protected SetStatement createInsertIdSetStatement(GraphqlParser.TypeContext typeContext) {
        String idVariableName = "@" + getIdVariableName(typeContext);
        Function function = new Function();
        function.setName("LAST_INSERT_ID");
        return new SetStatement(idVariableName, function);
    }

    protected UserVariable createInsertIdUserVariable(GraphqlParser.TypeContext typeContext) {
        String idVariableName = getIdVariableName(typeContext);
        UserVariable userVariable = new UserVariable();
        userVariable.setName(idVariableName);
        return userVariable;
    }
}
