package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrRegister register;

    public GraphqlMutationToStatements(GraphqlAntlrRegister register) {
        this.register = register;
    }

    public Statements createStatements(GraphqlParser.DocumentContext documentContext) {
        Statements statements = new Statements();
        statements.setStatements(documentContext.definition().stream().map(this::createStatement).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
        return statements;
    }

    protected Optional<Statement> createStatement(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.operationDefinition() == null) {
            return Optional.empty();
        }
        return operationDefinitionToStatement(definitionContext.operationDefinition());
    }

    protected Optional<Statement> operationDefinitionToStatement(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().getText().equals("mutation")) {
            operationDefinitionContext.selectionSet().selection().stream().map(selectionContext -> selectionToStatements(null, selectionContext));

            if (operationDefinitionContext.name() != null) {
                //TODO
            }
            if (operationDefinitionContext.variableDefinitions() != null) {
                //TODO
            }
            if (operationDefinitionContext.directives() != null) {
                //TODO
            }
        }
        return Optional.empty();
    }

    protected Statement selectionToStatements(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? register.getMutationTypeName() : register.getFieldTypeName(typeContext);
        Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());

        if (fieldTypeContext.isPresent()) {
            if (fieldDefinitionContext.isPresent()) {
                return argumentsToStatement(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
            }
        }
        return null;
    }

    protected Statement argumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> idField = getIdFieldArgument(fieldTypeContext, argumentsContext);
        if (idField.isPresent()) {
            return argumentsToUpdate(fieldTypeContext, idField.get(), argumentsDefinitionContext, argumentsContext);
        } else {
            return argumentsToInsert(fieldTypeContext, argumentsDefinitionContext, argumentsContext);
        }
    }

    protected Update argumentsToUpdate(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentContext idField, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Update update = new Update();
        update.setColumns(argumentsDefinitionContext.inputValueDefinition().stream().map(inputValueDefinitionContext -> argumentsToColumn(inputValueDefinitionContext, argumentsContext)).collect(Collectors.toList()));
        update.setExpressions(argumentsDefinitionContext.inputValueDefinition().stream().map(inputValueDefinitionContext -> argumentsToDBValue(inputValueDefinitionContext, argumentsContext)).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        update.setTable(table);
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(table, DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(idField.name().getText())));
        equalsTo.setRightExpression(register.scalarValueWithVariableToDBValue(idField.valueWithVariable()));
        update.setWhere(equalsTo);
        return update;
    }

    protected Insert argumentsToInsert(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Insert insert = new Insert();
        insert.setColumns(argumentsDefinitionContext.inputValueDefinition().stream().map(inputValueDefinitionContext -> argumentsToColumn(inputValueDefinitionContext, argumentsContext)).collect(Collectors.toList()));
        insert.setSetExpressionList(argumentsDefinitionContext.inputValueDefinition().stream().map(inputValueDefinitionContext -> argumentsToDBValue(inputValueDefinitionContext, argumentsContext)).collect(Collectors.toList()));
        String tableName = DBNameConverter.INSTANCE.graphqlTypeNameToTableName(register.getFieldTypeName(fieldTypeContext));
        Table table = new Table(tableName);
        insert.setTable(table);
        return insert;
    }

    protected Optional<GraphqlParser.ArgumentContext> getIdFieldArgument(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsContext argumentsContext) {
        String typeIdFieldName = register.getTypeIdFieldName(fieldTypeContext.typeName().name().getText());
        return argumentsContext.argument().stream().filter(argumentContext -> argumentContext.name().getText().equals(typeIdFieldName)).findFirst();
    }

    protected Column argumentsToColumn(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(argumentContext.get().name().getText()));
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                return new Column(DBNameConverter.INSTANCE.graphqlFieldNameToColumnName(inputValueDefinitionContext.name().getText()));
            }
        }
        //todo
        return null;
    }

    protected Expression argumentsToDBValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return register.scalarValueWithVariableToDBValue(argumentContext.get().valueWithVariable());
        } else {
            if (inputValueDefinitionContext.type().nonNullType() != null) {
                if (inputValueDefinitionContext.defaultValue() != null) {
                    return register.scalarValueToDBValue(inputValueDefinitionContext.defaultValue().value());
                }
            }
        }
        //todo
        return null;
    }
}
