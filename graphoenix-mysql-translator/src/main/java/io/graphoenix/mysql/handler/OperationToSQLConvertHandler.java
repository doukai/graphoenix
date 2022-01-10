package io.graphoenix.mysql.handler;

import io.graphoenix.core.manager.GraphQLVariablesProcessor;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple2;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class OperationToSQLConvertHandler {

    private final GraphQLQueryToSelect graphqlQueryToSelect;
    private final GraphQLMutationToStatements graphqlMutationToStatements;
    private final IGraphQLDocumentManager manager;
    private final GraphQLVariablesProcessor graphQLVariablesProcessor;


    @Inject
    public OperationToSQLConvertHandler(IGraphQLDocumentManager manager,
                                        GraphQLVariablesProcessor graphQLVariablesProcessor,
                                        GraphQLQueryToSelect graphqlQueryToSelect,
                                        GraphQLMutationToStatements graphqlMutationToStatements) {
        this.manager = manager;
        this.graphQLVariablesProcessor = graphQLVariablesProcessor;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
        this.graphqlMutationToStatements = graphqlMutationToStatements;
    }

    public String queryToSelect(String graphQL, Map<String, String> variables) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectSQL(graphQLVariablesProcessor.buildVariables(graphQL, variables));
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(String graphQL, Map<String, String> variables) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectsSQL(graphQLVariablesProcessor.buildVariables(graphQL, variables));
    }

    public Stream<String> mutationToStatements(String graphQL, Map<String, String> variables) {
        manager.registerFragment(graphQL);
        return this.graphqlMutationToStatements.createStatementsSQL(graphQLVariablesProcessor.buildVariables(graphQL, variables));
    }

    public String queryToSelect(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectSQL(graphQL);
    }

    public Stream<Tuple2<String, String>> querySelectionsToSelects(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectsSQL(graphQL);
    }

    public Stream<String> mutationToStatements(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlMutationToStatements.createStatementsSQL(graphQL);
    }
}
