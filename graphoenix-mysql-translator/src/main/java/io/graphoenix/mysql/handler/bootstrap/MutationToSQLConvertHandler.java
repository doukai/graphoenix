package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    @Override
    public Stream<String> transform(IGraphqlDocumentManager manager, Object graphQL) {
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        GraphqlMutationToStatements mutationToStatements = new GraphqlMutationToStatements(manager, graphqlQueryToSelect);
        return mutationToStatements.createStatementsSQL((String) graphQL);
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
