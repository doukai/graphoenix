package io.graphoenix.mysql.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.vavr.Tuple2;

import javax.inject.Inject;
import java.util.stream.Stream;

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
        return graphqlQueryToSelect.createSelectSQL(operationDefinitionContext);
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return graphqlQueryToSelect.createSelectsSQL(operationDefinitionContext);
    }

    public Stream<String> mutationToStatements(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return graphqlMutationToStatements.createStatementsSQL(operationDefinitionContext);
    }

    public String queryToSelect(String graphQL) {
        return graphqlQueryToSelect.createSelectSQL(graphQL);
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(String graphQL) {
        return graphqlQueryToSelect.createSelectsSQL(graphQL);
    }

    public Stream<String> mutationToStatements(String graphQL) {
        return graphqlMutationToStatements.createStatementsSQL(graphQL);
    }
}
