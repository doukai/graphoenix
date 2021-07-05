package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrRegister register;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlMutationToStatements(GraphqlAntlrRegister register, GraphqlArgumentsToWhere argumentsToWhere) {
        this.register = register;
        this.argumentsToWhere = argumentsToWhere;
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

    protected Stream<Statement> selectionToStatements(GraphqlParser.TypeContext typeContext, GraphqlParser.SelectionContext selectionContext) {
        String typeName = typeContext == null ? register.getMutationTypeName() : register.getFieldTypeName(typeContext);
        Optional<GraphqlParser.TypeContext> fieldTypeContext = register.getObjectFieldTypeContext(typeName, selectionContext.field().name().getText());
        Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = register.getObjectFieldDefinitionContext(typeName, selectionContext.field().name().getText());

        if (fieldTypeContext.isPresent()) {

            if (fieldDefinitionContext.isPresent()) {

                return argumentsToStatements(fieldTypeContext.get(), fieldDefinitionContext.get().argumentsDefinition(), selectionContext.field().arguments());
            }
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentsToStatements(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
       return argumentsDefinitionContext.inputValueDefinition().stream().flatMap(inputValueDefinitionContext -> argumentsToStatement(fieldTypeContext,inputValueDefinitionContext,argumentsContext));
    }

    protected Stream<Statement> argumentsToStatement(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentsContext argumentsContext) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = register.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return argumentToExpression(fieldTypeContext, inputValueDefinitionContext, argumentContext.get());
        } else {
//            return defaultValueToExpression(fieldTypeContext, inputValueDefinitionContext);
        }
        return Stream.empty();
    }

    protected Stream<Statement> argumentToExpression(GraphqlParser.TypeContext fieldTypeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ArgumentContext argumentContext) {
        return Stream.empty();
    }


}
