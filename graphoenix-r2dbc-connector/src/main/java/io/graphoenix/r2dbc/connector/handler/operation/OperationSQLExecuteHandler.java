package io.graphoenix.r2dbc.connector.handler.operation;

import com.google.gson.Gson;
import io.graphoenix.spi.dto.GraphQLResultBuilder;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import reactor.core.publisher.Mono;

import io.graphoenix.spi.dto.GraphQLResponse;

import java.util.Map;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class OperationSQLExecuteHandler implements IOperationHandler {

    private QueryExecutor queryExecutor;

    private MutationExecutor mutationExecutor;

    @Override
    public void setupManager(IGraphqlDocumentManager manager) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        this.queryExecutor = new QueryExecutor(connectionCreator);
        this.mutationExecutor = new MutationExecutor(connectionCreator);
    }

    @Override
    public GraphQLResponse query(Object sqlStatements) {
        Mono<String> jsonResult = queryExecutor.executeQuery(((SQLStatements) sqlStatements).getSqlStatements().get(0));
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public GraphQLResponse mutation(Object sqlStatements) {
        Mono<String> jsonResult = mutationExecutor.executeMutations(((SQLStatements) sqlStatements).getSqlStatements());
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public GraphQLResponse subscription(Object sqlStatements) {
        return null;
    }
}
