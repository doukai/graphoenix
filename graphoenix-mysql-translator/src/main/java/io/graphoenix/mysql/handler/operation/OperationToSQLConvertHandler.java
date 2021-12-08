package io.graphoenix.mysql.handler.operation;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.handler.IPipelineContext;
import org.javatuples.Pair;

import javax.inject.Inject;
import java.util.stream.Stream;

public class OperationToSQLConvertHandler implements IOperationHandler {

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

    @Override
    public boolean query(IPipelineContext context) {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        String sql = this.graphqlQueryToSelect.createSelectSQL(graphQL);
        context.add(sql);
        return false;
    }

    @Override
    public boolean queryAsync(IPipelineContext context) {
        query(context);
        return false;
    }

    @Override
    public boolean querySelectionsAsync(IPipelineContext context) {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        Stream<Pair<String, String>> sqlPair = this.graphqlQueryToSelect.createSelectsSQL(graphQL);
        context.add(sqlPair);
        return false;
    }

    @Override
    public boolean mutation(IPipelineContext context) {
        String graphQL = context.poll(String.class);
        manager.registerFragment(graphQL);
        Stream<String> sqlStream = this.graphqlMutationToStatements.createStatementsSQL(graphQL);
        context.add(sqlStream);
        return false;
    }

    @Override
    public boolean mutationAsync(IPipelineContext context) {
        mutation(context);
        return false;
    }

    @Override
    public boolean subscription(IPipelineContext context) {
        //TODO
        return false;
    }
}
