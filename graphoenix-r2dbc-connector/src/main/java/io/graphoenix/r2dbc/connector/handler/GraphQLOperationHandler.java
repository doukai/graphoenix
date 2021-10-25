package io.graphoenix.r2dbc.connector.handler;

import com.google.gson.Gson;
import io.graphoenix.common.config.GraphQLResultBuilder;
import io.graphoenix.r2dbc.connector.MutationExecutor;
import io.graphoenix.r2dbc.connector.QueryExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import reactor.core.publisher.Mono;

import java.graphoenix.meta.dto.GraphQLRequestBody;
import java.graphoenix.meta.dto.GraphQLResult;
import java.graphoenix.meta.spi.IGraphQLOperationHandler;
import java.graphoenix.meta.spi.IGraphQLToSQLHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static io.graphoenix.common.config.YamlConfigLoader.YAML_CONFIG_LOADER;

public class GraphQLOperationHandler implements IGraphQLOperationHandler {

    private final IGraphQLToSQLHandler graphQLToSQLHandler;

    private final QueryExecutor queryExecutor;

    private final MutationExecutor mutationExecutor;

    public GraphQLOperationHandler(IGraphQLToSQLHandler graphQLToSQLHandler) {

        this.graphQLToSQLHandler = graphQLToSQLHandler;
        ConnectionConfiguration connectionConfiguration = YAML_CONFIG_LOADER.loadAs("application.yaml", ConnectionConfiguration.class);
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
