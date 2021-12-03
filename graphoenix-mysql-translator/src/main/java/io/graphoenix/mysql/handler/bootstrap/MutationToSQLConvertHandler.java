package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.common.manager.GraphQLFieldMapManager;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import java.util.stream.Stream;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    @Override
    public boolean execute(IPipelineContext context) {
        IGraphQLDocumentManager manager = context.getManager();
        GraphQLFieldMapManager mapper = new GraphQLFieldMapManager(manager);
        GraphQLQueryToSelect graphqlQueryToSelect = new GraphQLQueryToSelect(manager, mapper, new GraphQLArgumentsToWhere(manager, mapper));
        GraphQLMutationToStatements mutationToStatements = new GraphQLMutationToStatements(manager, mapper, graphqlQueryToSelect);
        String graphQL = context.poll(String.class);
        Stream<String> statementsSQL = mutationToStatements.createStatementsSQL(graphQL);
        context.add(statementsSQL);
        return true;
    }
}
