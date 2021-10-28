package io.graphoenix.r2dbc.connector.handler;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import io.graphoenix.common.config.GraphQLResultBuilder;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.ISQLHandler;
import io.graphoenix.r2dbc.connector.MutationExecutor;
import io.graphoenix.r2dbc.connector.QueryExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import reactor.core.publisher.Mono;

import io.graphoenix.spi.dto.GraphQLResult;

import java.util.Map;

import static io.graphoenix.common.config.YamlConfigLoader.YAML_CONFIG_LOADER;

@AutoService(ISQLHandler.class)
public class ReactiveSQLHandler implements ISQLHandler {

    private QueryExecutor queryExecutor;

    private MutationExecutor mutationExecutor;

    @Override
    public void assign(IGraphqlDocumentManager manager) {

        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_LOADER.loadAs(Hammurabi.CONFIG_FILE_NAME, ConnectionConfiguration.class));
        this.queryExecutor = new QueryExecutor(connectionCreator);
        this.mutationExecutor = new MutationExecutor(connectionCreator);
    }

    @Override
    public GraphQLResult query(SQLStatements sqlStatements) {
        Mono<String> jsonResult = queryExecutor.executeQuery(sqlStatements.getSqlStatements().get(0));
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public GraphQLResult mutation(SQLStatements sqlStatements) {
        Mono<String> jsonResult = mutationExecutor.executeMutations(sqlStatements.getSqlStatements());
        GraphQLResultBuilder resultBuilder = new GraphQLResultBuilder(new Gson().fromJson(jsonResult.block(), Map.class));
        return resultBuilder.build();
    }

    @Override
    public GraphQLResult subscription(SQLStatements sqlStatements) {
        return null;
    }
}
