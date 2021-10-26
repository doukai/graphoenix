package io.graphoenix.r2dbc.connector.handler;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import io.graphoenix.common.config.GraphQLResultBuilder;
import io.graphoenix.r2dbc.connector.MutationExecutor;
import io.graphoenix.r2dbc.connector.QueryExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import reactor.core.publisher.Mono;

import io.graphoenix.meta.dto.GraphQLRequestBody;
import io.graphoenix.meta.dto.GraphQLResult;
import io.graphoenix.meta.spi.IGraphQLOperationHandler;
import io.graphoenix.meta.spi.IGraphQLToSQLHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static io.graphoenix.common.config.HandlerFactory.HANDLER_FACTORY;

@AutoService(IGraphQLToSQLHandler.class)
public class GraphQLOperationHandler implements IGraphQLOperationHandler {

    private final IGraphQLToSQLHandler graphQLToSQLHandler;

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    public GraphQLOperationHandler(ConnectionConfiguration connectionConfiguration) {

        this.graphQLToSQLHandler = HANDLER_FACTORY.create(IGraphQLToSQLHandler.class);
        ConnectionCreator connectionCreator = new ConnectionCreator(connectionConfiguration);
        this.queryExecutor = new QueryExecutor(connectionCreator);
        this.mutationExecutor = new MutationExecutor(connectionCreator);
    }

    @Override
    public GraphQLResult query(GraphQLRequestBody requestBody) {

        String selectSQL = graphQLToSQLHandler.queryOperationToSelectSQL(requestBody.getQuery());
        Mono<String> jsonResult = queryExecutor.executeQuery(selectSQL);
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public GraphQLResult mutation(GraphQLRequestBody requestBody) {

        List<String> mergeSQLList = graphQLToSQLHandler.mutationOperationToMergeSQLList(requestBody.getQuery());
        Mono<String> jsonResult = mutationExecutor.executeMutations(mergeSQLList);
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public Future<GraphQLResult> subscription(GraphQLRequestBody requestBody) {
        return null;
    }
}
