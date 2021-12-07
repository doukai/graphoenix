package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.util.stream.Stream;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;

    @Inject
    public MutationToSQLConvertHandler(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper) {
        this.manager = manager;
        this.mapper = mapper;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        GraphQLQueryToSelect graphqlQueryToSelect = new GraphQLQueryToSelect(manager, mapper, new GraphQLArgumentsToWhere(manager, mapper));
        GraphQLMutationToStatements mutationToStatements = new GraphQLMutationToStatements(manager, mapper, graphqlQueryToSelect);
        String graphQL = context.poll(String.class);
        Stream<String> statementsSQL = mutationToStatements.createStatementsSQL(graphQL);
        context.add(statementsSQL);
        return false;
    }
}
