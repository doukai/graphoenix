package io.graphoenix.mysql.handler;

import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.bootstrap.IMutationToSQLConvertHandler;

public class MutationToSQLConvertHandler implements IMutationToSQLConvertHandler {

    @Override
    public SQLStatements transform(IGraphqlDocumentManager manager, String graphQL) {
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        GraphqlMutationToStatements mutationToStatements = new GraphqlMutationToStatements(manager, graphqlQueryToSelect);
        return new SQLStatements(mutationToStatements.createStatementsSql(graphQL));
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {

    }
}
