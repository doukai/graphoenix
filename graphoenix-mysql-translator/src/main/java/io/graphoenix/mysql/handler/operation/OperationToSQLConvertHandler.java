package io.graphoenix.mysql.handler.operation;

import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple2;

import javax.inject.Inject;
import java.util.stream.Stream;

public class OperationToSQLConvertHandler {

    private final GraphQLQueryToSelect graphqlQueryToSelect;
    private final GraphQLMutationToStatements graphqlMutationToStatements;
    private final IGraphQLDocumentManager manager;


    @Inject
    public OperationToSQLConvertHandler(IGraphQLDocumentManager manager,
                                        GraphQLQueryToSelect graphqlQueryToSelect,
                                        GraphQLMutationToStatements graphqlMutationToStatements) {
        this.manager = manager;
        this.graphqlQueryToSelect = graphqlQueryToSelect;
        this.graphqlMutationToStatements = graphqlMutationToStatements;
    }

    public String query(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectSQL(graphQL);
    }

    public Stream<Tuple2<String, String>> querySelectionsAsync(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlQueryToSelect.createSelectsSQL(graphQL);
    }

    public Stream<String> mutation(String graphQL) {
        manager.registerFragment(graphQL);
        return this.graphqlMutationToStatements.createStatementsSQL(graphQL);
    }
}
