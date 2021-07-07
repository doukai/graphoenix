package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

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
        statements.setStatements(documentContext.definition().stream().flatMap(this::createStatement).collect(Collectors.toList()));
        return statements;
    }

    protected Stream<Statement> createStatement(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() != null) {
            return operationDefinitionToStatement(definitionContext.operationDefinition());
        }
        return Stream.empty();
    }

    protected Stream<Statement> operationDefinitionToStatement(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
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
            return operationDefinitionContext.selectionSet().selection().stream().flatMap(selectionContext -> selectionToStatements(null, selectionContext));
        }
        return Stream.empty();
    }

    protected Stream<Statement> selectionToStatements(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? register.getMutationTypeName() : register.getFieldTypeName(typeContext);
        Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());

        if (fieldTypeContext.isPresent()) {
            if (fieldDefinitionContext.isPresent()) {
                return argumentsToStatement(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                .filter(inputValueDefinitionContext -> register.isInnerScalar(inputValueDefinitionContext.type().getText())).collect(Collectors.toList());

        return Stream.concat(
                Stream.concat(
                        Stream.of(argumentsToStatement(fieldTypeContext, scalarInputValueDefinitionContextList, argumentsContext)),
                        argumentsDefinitionContext.inputValueDefinition().stream()
                                .filter(inputValueDefinitionContext -> !register.fieldTypeIsList(inputValueDefinitionContext.type()))
                                .filter(inputValueDefinitionContext -> register.isInputObject(inputValueDefinitionContext.type().getText()))
                                .flatMap(inputValueDefinitionContext -> inputObjectToStatement(fieldTypeContext, inputValueDefinitionContext, argumentsContext))
                ),
                argumentsDefinitionContext.inputValueDefinition().stream()
                        .filter(inputValueDefinitionContext1 -> register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                        .flatMap(inputValueDefinitionContext1 -> listTypeArgumentsToStatement(fieldTypeContext, inputValueDefinitionContext1, argumentsContext))
        );
    }

    protected Stream<Statement> objectValueWithVariableToStatement(GraphqlParser.TypeContext typeContext, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
        GraphqlParser.TypeDefinitionContext typeDefinitionContext = register.getDefinition(register.getFieldTypeName(inputValueDefinitionContext.type()));

        List<GraphqlParser.InputValueDefinitionContext> scalarInputValueDefinitionContextList = typeDefinitionContext.inputObjectTypeDefinition().inputObjectValueDefinitions().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext1 -> !register.fieldTypeIsList(inputValueDefinitionContext1.type()))
                .filter(inputValueDefinitionContext1 -> register.isInnerScalar(inputValueDefinitionContext1.type().getText())).collect(Collectors.toList());
        //todo
        return Stream.concat(
                Stream.concat(
                        Stream.of(inputObjectToStatement(fieldTypeContext, scalarInputValueDefinitionContextList, objectValueWithVariableContext)),
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

    protected Stream<Statement> inputObjectToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {

        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinition = register.getDefinition(register.getFieldTypeName(fieldTypeContext)).objectTypeDefinition();

        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = objectTypeDefinition.fieldsDefinition().fieldDefinition().stream().filter(fieldDefinitionContext1 ->
                fieldDefinitionContext1.name().getText().equals(inputValueDefinitionContext.name().getText())).findFirst();

        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);

        if (argumentContext.isPresent()) {
            if (argumentContext.get().valueWithVariable().objectValueWithVariable() != null) {
                if (fieldDefinitionContext.isPresent()) {
                    return objectValueWithVariableToStatement(fieldTypeContext, fieldDefinitionContext.get().type(), inputValueDefinitionContext, argumentContext.get().valueWithVariable().objectValueWithVariable());
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
                    return objectValueWithVariableToStatement(fieldTypeContext, fieldDefinitionContext.get().type(), inputValueDefinitionContext, objectFieldWithVariableContext.get().valueWithVariable().objectValueWithVariable());
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
                    return argumentContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                            .flatMap(valueWithVariableContext -> objectValueWithVariableToStatement(fieldTypeContext, fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable()));
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
                    return objectFieldWithVariableContext.get().valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                            .flatMap(valueWithVariableContext -> objectValueWithVariableToStatement(fieldTypeContext, fieldDefinitionContext.get().type(), inputValueDefinitionContext, valueWithVariableContext.objectValueWithVariable()));
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

    protected Statement argumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ArgumentsContext argumentsContext) {

        Optional<GraphqlParser.ArgumentContext> idField = getIdArgument(fieldTypeContext, argumentsContext);
        if (idField.isPresent()) {
            return argumentsToUpdate(fieldTypeContext, idField.get(), inputValueDefinitionContextList, argumentsContext);
        } else {
            return argumentsToInsert(fieldTypeContext, inputValueDefinitionContextList, argumentsContext);
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

    protected Statement inputObjectToStatement(GraphqlParser.TypeContext fieldTypeContext, List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {

        Optional<GraphqlParser.ObjectFieldWithVariableContext> idField = getIdObjectFieldWithVariable(fieldTypeContext, objectValueWithVariableContext);
        if (idField.isPresent()) {
            return inputObjectToUpdate(fieldTypeContext, idField.get(), inputValueDefinitionContextList, objectValueWithVariableContext);
        } else {
            return inputObjectToInsert(fieldTypeContext, inputValueDefinitionContextList, objectValueWithVariableContext);
        }
    }

    protected Update inputObjectToRelationUpdate(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext idField, GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext) {
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
        if (idField == null) {
            Function function = new Function();
            function.setName("LAST_INSERT_ID");
            equalsTo.setRightExpression(function);
        } else {
            equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        }
        update.setWhere(equalsTo);
        return update;
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
