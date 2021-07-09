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

    protected Stream<Statement> argumentsToStatementStream(GraphqlParser.TypeContext mutationFieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> singleTypeScalarInputValueDefinitionContextList = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> register.isInnerScalar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        argumentsToStatement(mutationFieldTypeContext, singleTypeScalarInputValueDefinitionContextList, argumentsContext),
                        argumentsDefinitionContext.inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                                .filter(inputValueDefinitionContext -> register.isInputObject(inputValueDefinitionContext.type().getText()))
                                .flatMap(inputValueDefinitionContext -> singleInputObjectToStatementStream(mutationFieldTypeContext, inputValueDefinitionContext, argumentsContext))
                ),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                        .flatMap(inputValueDefinitionContext1 -> listTypeArgumentsToStatement(mutationFieldTypeContext, inputValueDefinitionContext1, argumentsContext))
        );
    }

    protected Stream<Statement> objectValueWithVariableToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));

        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 -> !register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                .filter(inputValueDefinitionContext1 -> register.isInnerScalar(inputValueDefinitionContext1.type().getText())).collect(Collectors.toList());
        //todo
        return Stream.concat(
                Stream.concat(
                        Stream.concat(inputObjectToStatement(fieldTypeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(inputObjectToRelationUpdate(typeContext, idValueWithVariableContext, fieldTypeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext1 -> !register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                                .filter(inputValueDefinitionContext1 -> register.isInputObject(inputValueDefinitionContext1.type().getText()))
                                .flatMap(inputValueDefinitionContext1 -> inputObjectToStatement(fieldTypeContext, inputValueDefinitionContext1, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                        .flatMap(inputValueDefinitionContext1 -> listTypeObjectValueWithVariableToStatement(fieldTypeContext, inputValueDefinitionContext1, objectValueWithVariableContext))
        );
    }

    protected Stream<Statement> listObjectValueWithVariableToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));

        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 -> !register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                .filter(inputValueDefinitionContext1 -> register.isInnerScalar(inputValueDefinitionContext1.type().getText())).collect(Collectors.toList());


        //todo
        return Stream.concat(
                Stream.concat(
                        Stream.concat(inputObjectToStatement(fieldTypeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext),
                                Stream.of(listInputObjectToRelationUpdate(typeContext, idValueWithVariableContext, fieldTypeContext, objectValueWithVariableContext))),
                        typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext1 -> !register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                                .filter(inputValueDefinitionContext1 -> register.isInputObject(inputValueDefinitionContext1.type().getText()))
                                .flatMap(inputValueDefinitionContext1 -> inputObjectToStatement(fieldTypeContext, inputValueDefinitionContext1, objectValueWithVariableContext))
                ),
                typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                        .flatMap(inputValueDefinitionContext1 -> listTypeObjectValueWithVariableToStatement(fieldTypeContext, inputValueDefinitionContext1, objectValueWithVariableContext))
        );
    }


    protected Stream<Statement> singleInputObjectToStatementStream(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);

        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().objectValueWithVariable() != null) {
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ArgumentContext> idArgument = getIdArgument(fieldTypeContext, argumentsContext);
                    if (idArgument.isPresent()) {
                        return objectValueWithVariableToStatement(fieldTypeContext, idArgument.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, argumentContext.get().valueWithVariable().objectValueWithVariable());
                    } else {
                        return objectValueWithVariableToStatement(fieldTypeContext, null, fieldDefinitionContext.get().type(), inputValueDefinitionContext, argumentContext.get().valueWithVariable().objectValueWithVariable());
                    }
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> inputObjectToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);

        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
                if (fieldDefinitionContext.isPresent()) {

                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idObjectFieldWithVariable = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
                    if (idObjectFieldWithVariable.isPresent()) {
                        return objectValueWithVariableToStatement(fieldTypeContext, idObjectFieldWithVariable.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                    } else {
                        return objectValueWithVariableToStatement(fieldTypeContext, null, fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
                    }
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> listTypeArgumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
                if (fieldDefinitionContext.isPresent()) {

                    Optional<GraphqlParser.ArgumentContext> idArgument = getIdArgument(fieldTypeContext, argumentsContext);
                    if (idArgument.isPresent()) {

                        return Stream.concat(argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                        .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                        .flatMap(valueWithVariableContext -> listObjectValueWithVariableToStatement(fieldTypeContext, idArgument.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                                Stream.of(listInputObjectToRelationDelete(fieldTypeContext, idArgument.get().valueWithVariable(), fieldDefinitionContext.get().type(),
                                        argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                                .map(valueWithVariableContext -> getIdObjectFieldWithVariable(fieldTypeContext, valueWithVariableContext.objectValueWithVariable()))
                                                .filter(Optional::isPresent)
                                                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.get().valueWithVariable())
                                                .collect(Collectors.toList())))
                        );
                    } else {
                        return Stream.concat(argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                        .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                        .flatMap(valueWithVariableContext -> listObjectValueWithVariableToStatement(fieldTypeContext, idArgument.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                                Stream.of(listInputObjectToRelationDelete(fieldTypeContext, null, fieldDefinitionContext.get().type(),
                                        argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                                .map(valueWithVariableContext -> getIdObjectFieldWithVariable(fieldTypeContext, valueWithVariableContext.objectValueWithVariable()))
                                                .filter(Optional::isPresent)
                                                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.get().valueWithVariable())
                                                .collect(Collectors.toList())))
                        );
                    }
                }
            } else {

            }
        }
        return Stream.empty();
    }


    protected Stream<Statement> listTypeObjectValueWithVariableToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = register.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            if (objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable() != null) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();
                Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                        fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();
                if (fieldDefinitionContext.isPresent()) {
                    Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
                    if (idField.isPresent()) {
                        return Stream.concat(objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                        .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                        .flatMap(valueWithVariableContext -> listObjectValueWithVariableToStatement(fieldTypeContext, idField.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                                Stream.of(listInputObjectToRelationDelete(fieldTypeContext, idField.get().valueWithVariable(), fieldDefinitionContext.get().type(),
                                        objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                                .map(valueWithVariableContext -> getIdObjectFieldWithVariable(fieldTypeContext, valueWithVariableContext.objectValueWithVariable()))
                                                .filter(Optional::isPresent)
                                                .map(objectFieldWithVariableContext1 -> objectFieldWithVariableContext1.get().valueWithVariable())
                                                .collect(Collectors.toList())))
                        );
                    } else {
                        return Stream.concat(objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                        .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                                        .flatMap(valueWithVariableContext -> listObjectValueWithVariableToStatement(fieldTypeContext, idField.get().valueWithVariable(), fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable())),
                                Stream.of(listInputObjectToRelationDelete(fieldTypeContext, null, fieldDefinitionContext.get().type(),
                                        objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                                .map(valueWithVariableContext -> getIdObjectFieldWithVariable(fieldTypeContext, valueWithVariableContext.objectValueWithVariable()))
                                                .filter(Optional::isPresent)
                                                .map(objectFieldWithVariableContext1 -> objectFieldWithVariableContext1.get().valueWithVariable())
                                                .collect(Collectors.toList())))
                        );
                    }
                }
            } else {

            }
        }
        return Stream.empty();
    }

    protected Optional<GraphqlParser.ArgumentContext> getIdArgument(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        String typeIdFieldName = register.getTypeIdFieldName(register.getFieldTypeName(fieldTypeContext));
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Stream<Statement> argumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> idField = getIdArgument(fieldTypeContext, argumentsContext);
        if (idField.isPresent()) {
            return Stream.of(argumentsToUpdate(fieldTypeContext, idField.get(), inputValueDefinitionContextList, argumentsContext));
        } else {
            return Stream.of(argumentsToInsert(fieldTypeContext, inputValueDefinitionContextList, argumentsContext), createInsertIdSetStatement(fieldTypeContext));
        }
    }

    protected Update argumentsToUpdate(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> argumentsToColumn(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> argumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert argumentsToInsert(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> argumentsToColumn(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> argumentsToDBValue(inputValueDefinitionContext, argumentsContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> argumentsToColumn(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
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

    protected Optional<Expression> argumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
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


    protected Optional<GraphqlParser.ObjectFieldWithVariableContext> getIdObjectFieldWithVariable(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String typeIdFieldName = register.getTypeIdFieldName(register.getFieldTypeName(fieldTypeContext));
        return objectValueWithVariableContext.objectFieldWithVariable().stream().filter(fieldWithVariableContext -> fieldWithVariableContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Stream<Statement> inputObjectToStatement(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
        if (idField.isPresent()) {
            return Stream.of(inputObjectToUpdate(fieldTypeContext, idField.get(), inputValueDefinitionContextList, objectValueWithVariableContext));
        } else {
            return Stream.of(inputObjectToInsert(fieldTypeContext, inputValueDefinitionContextList, objectValueWithVariableContext), createInsertIdSetStatement(fieldTypeContext));
        }
    }

    protected Update inputObjectToRelationUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(typeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(typeContext), register.getFieldTypeName(fieldTypeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
        if (fieldIdField.isPresent()) {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable())));
        } else {
            Function function = new Function();
            function.setName("LAST_INSERT_ID");
            update.setExpressions(Collections.singletonList(function));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(typeContext)))));
        if (idValueWithVariableContext == null) {
            equalsTo.setRightExpression(createInsertIdUserVariable(typeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idValueWithVariableContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Update listInputObjectToRelationUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        Update update = new Update();
        update.setColumns(Collections.singletonList(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeRelationFieldName(register.getFieldTypeName(fieldTypeContext), register.getFieldTypeName(typeContext))))));
        Optional<GraphqlParser.ObjectFieldWithVariableContext> fieldIdField = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
        if (idValueWithVariableContext == null) {
            update.setExpressions(Collections.singletonList(createInsertIdUserVariable(fieldTypeContext)));
        } else {
            update.setExpressions(Collections.singletonList(register.scalarValueWithVariableToDBValue(idValueWithVariableContext)));
        }
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(typeContext)))));
        if (fieldIdField.isPresent()) {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(fieldIdField.get().valueWithVariable()));
        } else {
            equalsTo.setRightExpression(createInsertIdUserVariable(fieldTypeContext));
        }
        update.setWhere(equalsTo);
        return update;
    }

    protected Delete listInputObjectToRelationDelete(GraphqlParser.TypeContext typeContext, GraphqlParser.ValueWithVariableContext idValueWithVariableContext, GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.ValueWithVariableContext> idValueWithVariableContextList) {
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        Delete delete = new Delete();
        delete.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(register.getTypeIdFieldName(register.getFieldTypeName(typeContext)))));
        if (idValueWithVariableContext == null) {
            equalsTo.setRightExpression(createInsertIdUserVariable(fieldTypeContext));
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idValueWithVariableContext));
        }
        InExpression inExpression = new InExpression();
        inExpression.setNot(true);
        inExpression.setRightItemsList(new ExpressionList(idValueWithVariableContextList.stream().map(register::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
        delete.setWhere(new MultiAndExpression(Arrays.asList(equalsTo, inExpression)));
        return delete;
    }

    protected String getIdVariableName(GraphqlParser.TypeContext typeContext) {
        String typeName = register.getFieldTypeName(typeContext);
        return DBNameConverter.INSTANCE.graphqlFieldNameToVariableName(typeName, register.getTypeIdFieldName(typeName));
    }

    protected SetStatement createInsertIdSetStatement(GraphqlParser.TypeContext typeContext) {
        String idVariableName = getIdVariableName(typeContext);
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

    protected Update inputObjectToUpdate(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectFieldWithVariableContext idField, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Update update = new Update();
        update.setColumns(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> inputObjectToColumn(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        update.setExpressions(inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> register.isInnerScalar(register.getFieldTypeName(inputValueDefinitionContext.type()))).map(inputValueDefinitionContext -> inputObjectToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert inputObjectToInsert(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        Insert insert = new Insert();
        insert.setColumns(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> inputObjectToColumn(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        insert.setItemsList(new ExpressionList(inputValueDefinitionContextList.stream().map(inputValueDefinitionContext -> inputObjectToDBValue(inputValueDefinitionContext, objectValueWithVariableContext)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<Column> inputObjectToColumn(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
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

    protected Optional<Expression> inputObjectToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
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
}
