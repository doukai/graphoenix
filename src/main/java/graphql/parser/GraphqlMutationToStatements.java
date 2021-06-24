package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

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
}
