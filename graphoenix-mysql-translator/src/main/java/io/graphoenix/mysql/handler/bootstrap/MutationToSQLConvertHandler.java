package io.graphoenix.mysql.handler.bootstrap;

import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IPipelineContext;

import javax.inject.Inject;
import java.util.stream.Stream;

public class MutationToSQLConvertHandler implements IBootstrapHandler {

    private final GraphQLMutationToStatements mutationToStatements;

    @Inject
    public MutationToSQLConvertHandler(GraphQLMutationToStatements mutationToStatements) {
        this.mutationToStatements = mutationToStatements;
    }

    @Override
    public boolean execute(IPipelineContext context) {
        String graphQL = context.poll(String.class);
        Stream<String> statementsSQL = mutationToStatements.createStatementsSQL(graphQL);
        context.add(statementsSQL);
        return false;
    }
}
