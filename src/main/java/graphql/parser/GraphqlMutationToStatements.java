package graphql.parser;

import graphql.parser.antlr.GraphqlParser;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlMutationToStatements {

    private final GraphqlAntlrRegister register;
    private final GraphqlArgumentsToWhere argumentsToWhere;

    public GraphqlMutationToStatements(GraphqlAntlrRegister register, GraphqlArgumentsToWhere argumentsToWhere) {
        this.register = register;
        this.argumentsToWhere = argumentsToWhere;
    }
}
