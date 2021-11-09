package io.graphoenix.r2dbc.connector.handler.operation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;
import io.graphoenix.spi.dto.SelectionResult;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

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
    public JsonObject query(Object sql) throws Exception {
        return queryAsync(sql).block();
    }

    @Override
    public Mono<JsonObject> queryAsync(Object sql) throws Exception {
        return queryExecutor.executeQuery((String) sql).map(result -> new Gson().fromJson(result, JsonObject.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<SelectionResult<JsonObject>> querySelectionsAsync(Object sqlStream) throws Exception {
        return queryExecutor.executeQuery((Stream<Map.Entry<String, String>>) sqlStream).map(result -> new SelectionResult<>(result.getKey(), new Gson().fromJson(result.getValue(), JsonObject.class)));
    }

    @Override
    public JsonObject mutation(Object sqlStream) throws Exception {
        return mutationAsync(sqlStream).block();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<JsonObject> mutationAsync(Object sqlStream) throws Exception {
        return mutationExecutor.executeMutations((Stream<String>) sqlStream).map(result -> new Gson().fromJson(result, JsonObject.class));
    }

    @Override
    public Mono<JsonObject> subscription(Object sql) throws Exception {
        return queryAsync(sql);
    }
}
