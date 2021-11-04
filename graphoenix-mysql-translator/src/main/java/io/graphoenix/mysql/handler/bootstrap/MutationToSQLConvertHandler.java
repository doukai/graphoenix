package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.IBootstrapHandler;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    @Override
    public SQLStatements transform(IGraphqlDocumentManager manager, Object graphQL) {
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(manager, new GraphqlArgumentsToWhere(manager));
        GraphqlMutationToStatements mutationToStatements = new GraphqlMutationToStatements(manager, graphqlQueryToSelect);
        return new SQLStatements(mutationToStatements.createStatementsSql((String) graphQL));
    }

    @Override
    public void process(IGraphqlDocumentManager manager) {
    }
}
