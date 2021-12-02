package io.graphoenix.r2dbc.connector.handler.operation;

import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.QueryExecutor;
import io.graphoenix.spi.config.R2DBCConfig;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import io.graphoenix.spi.handler.IPipelineContext;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

@SuppressWarnings("unchecked")
public class OperationSQLExecuteHandler implements IOperationHandler {

    private QueryExecutor queryExecutor;

    private MutationExecutor mutationExecutor;

    @Override
    public void init(IPipelineContext context) {
        ConnectionCreator connectionCreator = new ConnectionCreator(YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, R2DBCConfig.class));
        this.queryExecutor = new QueryExecutor(connectionCreator);
        this.mutationExecutor = new MutationExecutor(connectionCreator);
    }

    @Override
    public void query(IPipelineContext context) throws Exception {
        String sql = context.poll(String.class);
        Map<String, Object> parameters = context.poll(Map.class);
        String result = queryExecutor.executeQuery(sql, parameters).block();
        context.add(result);
    }

    @Override
    public void queryAsync(IPipelineContext context) throws Exception {
        String sql = context.poll(String.class);
        Map<String, Object> parameters = context.poll(Map.class);
        Mono<String> result = queryExecutor.executeQuery(sql, parameters);
        context.add(result);
    }

    @Override
    public void querySelectionsAsync(IPipelineContext context) throws Exception {
        Stream<Pair<String, String>> sqlStream = context.poll(Stream.class);
        Map<String, Object> parameters = context.poll(Map.class);
        Flux<Pair<String, String>> result = queryExecutor.executeQuery(sqlStream, parameters);
        context.add(result);
    }

    @Override
    public void mutation(IPipelineContext context) throws Exception {
        Stream<String> sqlStream = context.poll(Stream.class);
        Map<String, Object> parameters = context.poll(Map.class);
        String result = mutationExecutor.executeMutations(sqlStream, parameters).block();
        context.add(result);
    }

    @Override
    public void mutationAsync(IPipelineContext context) throws Exception {
        Stream<String> sqlStream = context.poll(Stream.class);
        Map<String, Object> parameters = context.poll(Map.class);
        Mono<String> result = mutationExecutor.executeMutations(sqlStream, parameters);
        context.add(result);
    }

    @Override
    public void subscription(IPipelineContext context) throws Exception {
        //TODO
    }
}
