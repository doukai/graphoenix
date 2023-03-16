package io.graphoenix.mysql.translator.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mysql.translator.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.translator.GraphQLQueryToSelect;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.util.stream.Stream;

@ApplicationScoped
public class OperationToSQLConvertHandler {

    private final GraphQLQueryToSelect graphqlQueryToSelect;

    private final GraphQLMutationToStatements graphqlMutationToStatements;

    @Inject
    public OperationToSQLConvertHandler(GraphQLQueryToSelect graphqlQueryToSelect,
                                        GraphQLMutationToStatements graphqlMutationToStatements) {
        this.graphqlQueryToSelect = graphqlQueryToSelect;
        this.graphqlMutationToStatements = graphqlMutationToStatements;
    }

    public String queryToSelect(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Logger.debug("translate query operation:\r\n{}", operationDefinitionContext.getText());
        String selectSQL = graphqlQueryToSelect.createSelectSQL(operationDefinitionContext);
        Logger.debug("translate to sql:\r\n{}", selectSQL);
        return selectSQL;
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Logger.debug("translate query operation:\r\n{}", operationDefinitionContext.getText());
        return graphqlQueryToSelect.createSelectsSQL(operationDefinitionContext);
    }

    public Stream<String> mutationToStatements(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        Logger.debug("translate mutation operation:\r\n{}", operationDefinitionContext.getText());
        return graphqlMutationToStatements.createStatementsSQL(operationDefinitionContext);
    }

    public String queryToSelect(String graphQL) {
        Logger.debug("translate query operation:\r\n{}", graphQL);
        String selectSQL = graphqlQueryToSelect.createSelectSQL(graphQL);
        Logger.debug("translate to sql:\r\n{}", selectSQL);
        return selectSQL;
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(String graphQL) {
        Logger.debug("translate query operation:\r\n{}", graphQL);
        return graphqlQueryToSelect.createSelectsSQL(graphQL);
    }

    public Stream<String> mutationToStatements(String graphQL) {
        Logger.debug("translate mutation operation:\r\n{}", graphQL);
        return graphqlMutationToStatements.createStatementsSQL(graphQL);
    }
}
