package io.graphoenix.mysql;

import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.r2dbc.connector.MutationExecutor;
import io.graphoenix.r2dbc.connector.QueryExecutor;

import java.graphoenix.meta.dto.GraphQLRequestBody;
import java.graphoenix.meta.dto.GraphQLResult;
import java.graphoenix.meta.spi.IGraphQLOperationHandler;
import java.util.concurrent.Future;

public class MysqlReactiveHandler implements IGraphQLOperationHandler {

    private GraphqlQueryToSelect graphqlQueryToSelect;
    private GraphqlMutationToStatements graphqlMutationToStatements;
    private QueryExecutor queryExecutor;
    private MutationExecutor mutationExecutor;


    @Override
    public GraphQLResult query(GraphQLRequestBody requestBody) {
        queryExecutor.executeQuery(graphqlQueryToSelect.createSelectsSql(requestBody.getQuery()));

        return null;
    }

    @Override
    public GraphQLResult mutation(GraphQLRequestBody requestBody) {
        return null;
    }

    @Override
    public Future<GraphQLResult> subscription(GraphQLRequestBody requestBody) {
        return null;
    }
}
