package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.common.manager.GraphQLFieldMapManager;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;

import java.util.stream.Stream;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    @Override
    public Stream<String> transform(IGraphQLDocumentManager manager, Object graphQL) {
        GraphQLFieldMapManager mapper = new GraphQLFieldMapManager(manager);
        GraphQLQueryToSelect graphqlQueryToSelect = new GraphQLQueryToSelect(manager, mapper, new GraphQLArgumentsToWhere(manager, mapper));
        GraphQLMutationToStatements mutationToStatements = new GraphQLMutationToStatements(manager, mapper, graphqlQueryToSelect);
        return mutationToStatements.createStatementsSQL((String) graphQL);
    }
}
